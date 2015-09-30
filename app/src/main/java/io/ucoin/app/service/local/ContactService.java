package io.ucoin.app.service.local;

import android.app.Activity;
import android.app.Application;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import io.ucoin.app.dao.sqlite.SQLiteTable;
import io.ucoin.app.content.Provider;
import io.ucoin.app.model.local.Contact;
import io.ucoin.app.model.remote.Identity;
import io.ucoin.app.service.BaseService;
import io.ucoin.app.service.ServiceLocator;
import io.ucoin.app.service.exception.DuplicatePubkeyException;
import io.ucoin.app.technical.CollectionUtils;
import io.ucoin.app.technical.ObjectUtils;
import io.ucoin.app.technical.UCoinTechnicalException;

/**
 * Created by eis on 07/02/15.
 */
public class ContactService extends BaseService {

    /**
     * Logger.
     */
    private static final String TAG = "ContactService";

    // a cache instance of the contact Uri
    // Could NOT be static, because Uri is initialize in Provider.onCreate() method ;(
    private Uri mContentUri = null;
    private Uri mViewContentUri = null;

    private SelectCursorHolder mSelectHolder = null;
    private SelectViewCursorHolder mSelectViewHolder = null;
    private Contact2CurrencyService mContact2CurrencyService = null;

    public ContactService() {
        super();
    }

    @Override
    public void initialize() {
        super.initialize();
        mContact2CurrencyService = ServiceLocator.instance().getContact2CurrencyService();
    }

    public Contact save(final Context context, final Contact contact) throws DuplicatePubkeyException {
        ObjectUtils.checkNotNull(contact);

        // create if not exists
        if (contact.getId() == null) {

            insert(context.getContentResolver(), contact);
        }

        // or update
        else {
            update(context.getContentResolver(), contact);

        }

        // Save contact identities
        if (CollectionUtils.isNotEmpty(contact.getIdentities())) {
            mContact2CurrencyService.saveAll(context, contact.getId(), contact.getIdentities());
        }

        // return the updated contact (id could have change)
        return contact;
    }

    public List<Contact> getContacts(Activity activity) {
        return getContacts(activity.getApplication());
    }

    public List<Contact> getContacts(Application application) {
        long accountId = ((io.ucoin.app.Application) application).getAccountId();
        return getContactsByAccountId(application.getContentResolver(), accountId);
    }

    public List<Contact> getContactsByCurrencyId(Context context, long currencyId) {
        return getContactsByCurrencyId(context.getContentResolver(), currencyId);
    }

    public Contact getContactById(Context context, long contactId) {
        return getContactById(context.getContentResolver(), contactId);
    }

    public Contact getContactViewById(Context context, long contactId) {
        return getContactViewById(context.getContentResolver(), contactId);
    }

    public void delete(final Context context, final long contactId) {

        ContentResolver resolver = context.getContentResolver();

        String whereClause = "_id=?";
        String[] whereArgs = new String[]{String.valueOf(contactId)};
        int rowsUpdated = resolver.delete(Provider.CONTACT_URI, whereClause, whereArgs);
        if (rowsUpdated != 1) {
            throw new UCoinTechnicalException(String.format("Error while deleting contact [id=%s]. %s rows updated.", contactId, rowsUpdated));
        }
    }

    public Bitmap getPhotoAsBitmap(Context context, long phoneContactId, boolean largeScale) {
        InputStream is = null;
        if (largeScale) {
            is = getLargePhoto(context.getContentResolver(), phoneContactId);
        }
        else {
            is = getSmallPhoto(context.getContentResolver(), phoneContactId);
        }
        if (is != null) {
            try {
                return BitmapFactory.decodeStream(is);
            }
            finally {
                try {
                    is.close();
                }
                catch(IOException e) {
                    // silently mode
                }
            }
        }
        return null;
    }


    /* -- internal methods-- */

    protected List<Contact> getContactsByAccountId(ContentResolver resolver, long accountId) {

        String selection = SQLiteTable.Contact.ACCOUNT_ID + "=?";
        String[] selectionArgs = {
                String.valueOf(accountId)
        };
        String orderBy = SQLiteTable.Contact.NAME + " ASC";

        Cursor cursor = resolver.query(Provider.CONTACT_URI, new String[]{}, selection,
                selectionArgs, orderBy);

        List<Contact> result = new ArrayList<Contact>();
        while (cursor.moveToNext()) {
            Contact contact = toContact(cursor);
            result.add(contact);
        }
        cursor.close();

        return result;
    }

    protected List<Contact> getContactsByCurrencyId(ContentResolver resolver, long currencyId) {

        String selection = SQLiteTable.ContactView.CURRENCY_ID + "=?";
        String[] selectionArgs = {
                String.valueOf(currencyId),
        };
        String orderBy = SQLiteTable.ContactView.NAME + " ASC";

        Cursor cursor = resolver.query(Provider.CONTACT_VIEW_URI, new String[]{}, selection,
                selectionArgs, orderBy);

        List<Contact> result = new ArrayList<Contact>();
        while (cursor.moveToNext()) {
            Contact contact = toContactFromView(cursor);
            result.add(contact);
        }
        cursor.close();

        return result;
    }

    protected Contact getContactById(ContentResolver resolver, long contactId) {

        String selection = SQLiteTable.Contact._ID + "=?";
        String[] selectionArgs = {
                String.valueOf(contactId),
        };

        Cursor cursor = resolver.query(Provider.CONTACT_URI, new String[]{}, selection,
                selectionArgs, null);

        Contact result = null;
        if (cursor.moveToNext()) {
            result = toContact(cursor);
        }
        cursor.close();

        return result;
    }

    protected Contact getContactViewById(ContentResolver resolver, long contactId) {

        String selection = SQLiteTable.Contact._ID + "=?";
        String[] selectionArgs = {
                String.valueOf(contactId),
        };

        Cursor cursor = resolver.query(Provider.CONTACT_VIEW_URI, new String[]{}, selection,
                selectionArgs, null);

        Contact result = null;
        if (cursor.moveToNext()) {
            result = toContactFromView(cursor);
        }
        cursor.close();

        return result;
    }



    protected void insert(final ContentResolver resolver, final Contact source) {

        ContentValues target = toContentValues(source);

        Uri uri = resolver.insert(Provider.CONTACT_URI, target);
        Long contactId = ContentUris.parseId(uri);
        if (contactId < 0) {
            throw new UCoinTechnicalException("Error while inserting contact");
        }

        // Refresh the inserted account
        source.setId(contactId);
    }

    protected void update(final ContentResolver resolver, final Contact source) {
        ObjectUtils.checkNotNull(source.getId());

        ContentValues target = toContentValues(source);

        String whereClause = "_id=?";
        String[] whereArgs = new String[]{String.valueOf(source.getId())};
        int rowsUpdated = resolver.update(Provider.CONTACT_URI, target, whereClause, whereArgs);
        if (rowsUpdated != 1) {
            throw new UCoinTechnicalException(String.format("Error while updating contact. %s rows updated.", rowsUpdated));
        }
    }

    protected ContentValues toContentValues(final Contact source) {
        //Create account in database
        ContentValues target = new ContentValues();
        target.put(SQLiteTable.Contact.ACCOUNT_ID, source.getAccountId());
        target.put(SQLiteTable.Contact.NAME, source.getName());
        target.put(SQLiteTable.Contact.PHONE_CONTACT_ID, source.getPhoneContactId());
        return target;
    }

    protected Contact toContact(final Cursor cursor) {
        // Init the holder is need
        if (mSelectHolder == null) {
            mSelectHolder = new SelectCursorHolder(cursor);
        }

        Contact result = new Contact();
        result.setId(cursor.getLong(mSelectHolder.idIndex));
        result.setAccountId(cursor.getLong(mSelectHolder.accountIdIndex));
        result.setName(cursor.getString(mSelectHolder.nameIdIndex));
        result.setPhoneContactId(cursor.getLong(mSelectViewHolder.phoneContactId));

        return result;
    }

    public Contact toContactFromView(final Cursor cursor) {
        // Init the holder is need
        if (mSelectViewHolder == null) {
            mSelectViewHolder = new SelectViewCursorHolder(cursor);
        }

        Contact result = new Contact();
        result.setId(cursor.getLong(mSelectViewHolder.idIndex));
        result.setAccountId(cursor.getLong(mSelectViewHolder.accountIdIndex));
        result.setName(cursor.getString(mSelectViewHolder.nameIdIndex));
        result.setPhoneContactId(cursor.getLong(mSelectViewHolder.phoneContactId));

        Identity identity = new Identity();
        identity.setCurrencyId(cursor.getLong(mSelectViewHolder.currencyIdIndex));
        identity.setUid(cursor.getString(mSelectViewHolder.uidIdIndex));
        identity.setPubkey(cursor.getString(mSelectViewHolder.pubkeyIdIndex));
        result.addIdentity(identity);

        return result;
    }

    protected InputStream getSmallPhoto(ContentResolver contentResolver, long phoneContactId) {
        Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, phoneContactId);
        Uri photoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
        Cursor cursor = contentResolver.query(photoUri,
                new String[] {ContactsContract.Contacts.Photo.PHOTO}, null, null, null);
        if (cursor == null) {
            return null;
        }
        try {
            if (cursor.moveToFirst()) {
                byte[] data = cursor.getBlob(0);
                if (data != null) {
                    return new ByteArrayInputStream(data);
                }
            }
        } finally {
            cursor.close();
        }
        return null;
    }

    protected InputStream getLargePhoto(ContentResolver contentResolver, long phoneContactId) {

        Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, phoneContactId);
        Uri displayPhotoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.DISPLAY_PHOTO);
        try {
            AssetFileDescriptor fd =
                    contentResolver.openAssetFileDescriptor(displayPhotoUri, "r");
            return fd.createInputStream();
        } catch (IOException e) {
            return null;
        }

    }

    protected class SelectCursorHolder {

        int idIndex;
        int accountIdIndex;
        int nameIdIndex;
        int phoneContactId;

        private SelectCursorHolder(final Cursor cursor ) {
            idIndex = cursor.getColumnIndex(SQLiteTable.Contact._ID);
            accountIdIndex = cursor.getColumnIndex(SQLiteTable.Contact.ACCOUNT_ID);
            nameIdIndex = cursor.getColumnIndex(SQLiteTable.Contact.NAME);
            phoneContactId = cursor.getColumnIndex(SQLiteTable.Contact.PHONE_CONTACT_ID);
        }
    }

    protected class SelectViewCursorHolder extends SelectCursorHolder {

        int currencyIdIndex;
        int uidIdIndex;
        int pubkeyIdIndex;
        int phoneContactId;

        private SelectViewCursorHolder(final Cursor cursor ) {
            super(cursor);
            currencyIdIndex = cursor.getColumnIndex(SQLiteTable.Contact2Currency.CURRENCY_ID);
            uidIdIndex = cursor.getColumnIndex(SQLiteTable.Contact2Currency.UID);
            pubkeyIdIndex = cursor.getColumnIndex(SQLiteTable.Contact2Currency.PUBLIC_KEY);
            phoneContactId = cursor.getColumnIndex(SQLiteTable.Contact.PHONE_CONTACT_ID);
        }
    }

}
