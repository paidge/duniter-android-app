package io.ucoin.app.database;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper implements Contract {

    private static final String INTEGER = " INTEGER ";
    private static final String REAL = " REAL ";
    private static final String TEXT = " TEXT ";
    private static final String BLOB = " BLOB ";
    private static final String UNIQUE = " UNIQUE ";
    private static final String NOTNULL = " NOT NULL";
    private static final String COMMA = ",";


    public DatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        String CREATE_TABLE_ACCOUNT = "CREATE TABLE " + Account.TABLE_NAME + "(" +
                Account._ID + " INTEGER PRIMARY KEY AUTOINCREMENT" + COMMA +
                Account.PUBLIC_KEY + TEXT + NOTNULL + COMMA +
                Account.SALT + TEXT + NOTNULL + COMMA +
                Account.UID + TEXT + NOTNULL + COMMA +
                Account.CRYPT_PIN + TEXT + UNIQUE +  COMMA +
                UNIQUE + "(" + Account.UID + COMMA + Account.PUBLIC_KEY + ")" +
                ");";
        db.execSQL(CREATE_TABLE_ACCOUNT);

        String CREATE_TABLE_CURRENCY = "CREATE TABLE " + Currency.TABLE_NAME + "(" +
                Currency._ID + " INTEGER PRIMARY KEY AUTOINCREMENT" + COMMA +
                Currency.NAME + TEXT + NOTNULL + UNIQUE + COMMA +
                Currency.ACCOUNT_ID + TEXT + NOTNULL + COMMA +
                Currency.MEMBERS_COUNT + INTEGER + NOTNULL + COMMA +
                Currency.FIRST_BLOCK_SIGNATURE + TEXT + UNIQUE + NOTNULL + COMMA +
                "FOREIGN KEY (" + Currency.ACCOUNT_ID + ") REFERENCES " +
                Account.TABLE_NAME + "(" + Account._ID + ")" +
                ")";
        db.execSQL(CREATE_TABLE_CURRENCY);

        String CREATE_TABLE_WALLET = "CREATE TABLE " + Wallet.TABLE_NAME + "(" +
                Wallet._ID + " INTEGER PRIMARY KEY AUTOINCREMENT" + COMMA +
                Wallet.NAME + TEXT + COMMA +
                Wallet.UID + TEXT + COMMA +
                Wallet.SALT + TEXT + COMMA +
                Wallet.PUBLIC_KEY + TEXT + NOTNULL + COMMA +
                Wallet.CERT_TS + TEXT + COMMA +
                // TODO : change SECRET_KEY type to BLOB
                Wallet.SECRET_KEY + TEXT + COMMA +
                Wallet.ACCOUNT_ID + INTEGER + NOTNULL + COMMA +
                Wallet.CURRENCY_ID + INTEGER + NOTNULL + COMMA +
                Wallet.IS_MEMBER + INTEGER + NOTNULL + COMMA +
                Wallet.CREDIT + INTEGER + NOTNULL + COMMA +
                "FOREIGN KEY (" + Wallet.ACCOUNT_ID + ") REFERENCES " + Account.TABLE_NAME + "(" + Account._ID + ")" +
                "FOREIGN KEY (" + Wallet.CURRENCY_ID + ") REFERENCES " + Currency.TABLE_NAME + "(" + Currency._ID + ")" +
                UNIQUE + "(" + Wallet.CURRENCY_ID + COMMA + Wallet.PUBLIC_KEY + ")" +
                UNIQUE + "(" + Wallet.CURRENCY_ID + COMMA + Wallet.NAME + ")" +
                ")";
        db.execSQL(CREATE_TABLE_WALLET);

        String CREATE_TABLE_PEER = "CREATE TABLE " + Peer.TABLE_NAME + "(" +
                Peer._ID + " INTEGER PRIMARY KEY AUTOINCREMENT" + COMMA +
                Peer.CURRENCY_ID + INTEGER + NOTNULL + COMMA +
                Peer.HOST + TEXT + COMMA +
                //Peer.IPV4 + TEXT + COMMA +
                //Peer.IPV6 + TEXT + COMMA +
                Peer.PORT + INTEGER + COMMA +
                "FOREIGN KEY (" + Peer.CURRENCY_ID + ") REFERENCES " +
                Currency.TABLE_NAME + "(" + Currency._ID + ")" +
                ")";
        db.execSQL(CREATE_TABLE_PEER);

        String CREATE_TABLE_SOURCE = "CREATE TABLE " + Source.TABLE_NAME + "(" +
                Source._ID + " INTEGER PRIMARY KEY AUTOINCREMENT" + COMMA +
                Source.CURRENCY_NAME + TEXT + NOTNULL + COMMA +
                Source.WALLET_PUBLIC_KEY + TEXT + NOTNULL + COMMA +
                Source.FINGERPRINT + TEXT + NOTNULL + COMMA +
                Source.TYPE + TEXT + NOTNULL + COMMA +
                Source.AMOUNT + REAL + NOTNULL + COMMA +
                Source.BLOCK + INTEGER + NOTNULL + COMMA +
                UNIQUE + "(" + Source.FINGERPRINT + ")" + COMMA +
                "FOREIGN KEY (" + Source.CURRENCY_NAME + ") REFERENCES " +
                Currency.TABLE_NAME + "(" + Currency.NAME + ")" + COMMA +
                "FOREIGN KEY (" + Source.WALLET_PUBLIC_KEY + ") REFERENCES " +
                Wallet.TABLE_NAME + "(" + Wallet.PUBLIC_KEY + ")" +
                ")";
        db.execSQL(CREATE_TABLE_SOURCE);

        String CREATE_TABLE_TX = "CREATE TABLE " + Tx.TABLE_NAME + "(" +
                Tx._ID + " INTEGER PRIMARY KEY AUTOINCREMENT" + COMMA +
                Tx.CURRENCY_NAME + TEXT + NOTNULL + COMMA +
                Tx.COMMENT + TEXT + NOTNULL + COMMA +
                Tx.BLOCK + INTEGER +  COMMA +
                "FOREIGN KEY (" + Tx.CURRENCY_NAME + ") REFERENCES " +
                Currency.TABLE_NAME + "(" + Currency.NAME + ")" +
                ")";
        db.execSQL(CREATE_TABLE_TX);

        String CREATE_TABLE_TX_SIGNATURE = "CREATE TABLE " + TxSignature.TABLE_NAME + "(" +
                TxSignature._ID + " INTEGER PRIMARY KEY AUTOINCREMENT" + COMMA +
                TxSignature.TX_ID + INTEGER + NOTNULL + COMMA +
                TxSignature.VALUE + TEXT + NOTNULL + COMMA +
                TxSignature.ISSUER_ORDER + INTEGER + NOTNULL + COMMA +
                "FOREIGN KEY (" + TxSignature.TX_ID + ") REFERENCES " +
                Tx.TABLE_NAME + "(" + Tx._ID + ")" + COMMA +
                UNIQUE + "(" + TxSignature.TX_ID + COMMA + TxSignature.VALUE + ")" +
                ")";
        db.execSQL(CREATE_TABLE_TX_SIGNATURE);

        String CREATE_TABLE_TX_INPUT = "CREATE TABLE " + TxInput.TABLE_NAME + "(" +
                TxInput._ID + " INTEGER PRIMARY KEY AUTOINCREMENT" + COMMA +
                TxInput.TX_ID + INTEGER + NOTNULL + COMMA +
                TxInput.ISSUER_ORDER + INTEGER + NOTNULL + COMMA +
                TxInput.SOURCE_FINGERPRINT + TEXT + NOTNULL + COMMA +
                TxInput.KEY + TEXT + NOTNULL + COMMA +
                TxInput.SIGNATURE + TEXT + NOTNULL + COMMA +
                TxInput.AMOUNT + INTEGER + NOTNULL + COMMA +
                "FOREIGN KEY (" + TxInput.TX_ID + ") REFERENCES " +
                Tx.TABLE_NAME + "(" + Tx._ID + ")" + COMMA +
                "FOREIGN KEY (" + TxInput.SOURCE_FINGERPRINT + ") REFERENCES " +
                Source.TABLE_NAME + "(" + Source.FINGERPRINT + ")" + COMMA +
                "FOREIGN KEY (" + TxInput.ISSUER_ORDER + ") REFERENCES " +
                TxSignature.TABLE_NAME + "(" + TxSignature.ISSUER_ORDER + ")" +
                ")";
        db.execSQL(CREATE_TABLE_TX_INPUT);

        String CREATE_TABLE_TX_OUTPUT = "CREATE TABLE " + TxOutput.TABLE_NAME + "(" +
                TxOutput._ID + " INTEGER PRIMARY KEY AUTOINCREMENT" + COMMA +
                TxOutput.TX_ID + INTEGER + NOTNULL + COMMA +
                TxOutput.KEY + TEXT + NOTNULL + COMMA +
                TxOutput.AMOUNT + INTEGER + NOTNULL + COMMA +
                "FOREIGN KEY (" + TxOutput.TX_ID + ") REFERENCES " +
                Tx.TABLE_NAME + "(" + Tx._ID + ")" +
                ")";
        db.execSQL(CREATE_TABLE_TX_OUTPUT);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        // TODO: for DEV only :
        // Drop all tables
        db.execSQL("DROP TABLE IF EXISTS " + TxOutput.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TxInput.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TxSignature.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + Tx.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + Source.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + Peer.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + Currency.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + Wallet.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + Account.TABLE_NAME);

        // then recreate
        onCreate(db);
    }

}
