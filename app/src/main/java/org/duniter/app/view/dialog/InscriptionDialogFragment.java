package org.duniter.app.view.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/*import com.android.volley.NoConnectionError;
import com.android.volley.Response;
import com.android.volley.VolleyError;*/

import org.duniter.app.Application;
import org.duniter.app.R;
import org.duniter.app.model.Entity.Currency;
import org.duniter.app.technical.callback.CallbackBoolean;
import org.duniter.app.widget.InscriptionView;
import org.duniter.app.widget.SelectorCurrencyView;

public class InscriptionDialogFragment extends DialogFragment{

    private LinearLayout mFieldLayout;
    private LinearLayout mProgressLayout;

    private EditText mUid;
    private EditText mSalt;
    private EditText mPassword;
    private EditText mConfirmPassword;
    private AlertDialog alert;

    private SelectorCurrencyView selectorCurrencyView;
    private InscriptionView inscriptionView;

    private Currency currency;

    public static InscriptionDialogFragment newInstance() {
        InscriptionDialogFragment fragment = new InscriptionDialogFragment();
        fragment.setArguments(new Bundle());
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.dialog_fragment_inscription, null);
        builder.setView(view);
        builder.setTitle(getString(R.string.inscription_wallet));

        currency = null;

        mFieldLayout = (LinearLayout) view.findViewById(R.id.field_layout);
        mProgressLayout = (LinearLayout) view.findViewById(R.id.progress_layout);
        mUid = (EditText) view.findViewById(R.id.uid);
        mSalt = (EditText) view.findViewById(R.id.salt);
        mPassword = (EditText) view.findViewById(R.id.password);
        mPassword.setTypeface(Typeface.DEFAULT);
        mConfirmPassword = (EditText) view.findViewById(R.id.confirm_password);
        mConfirmPassword.setTypeface(Typeface.DEFAULT);

        selectorCurrencyView = new SelectorCurrencyView(false,getActivity(), view.findViewById(R.id.selector_currency), new SelectorCurrencyView.Action() {
            @Override
            public void currencyIdFind(Currency c) {
                currency = c;
                inscriptionView.validateWallet(c, getFragmentManager());
            }
        });
        inscriptionView = new InscriptionView(getActivity(), mUid, mSalt, mPassword, mConfirmPassword, selectorCurrencyView, new InscriptionView.Action() {
            @Override
            public void onFinish() {
                alert.dismiss();
            }

            @Override
            public void onError(boolean isUid) {
                if(isUid){
                    badUid();
                }else{
                    badPublicKey();
                }
            }
        });

        mConfirmPassword.setImeOptions(EditorInfo.IME_ACTION_DONE);
        mConfirmPassword.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    alert.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
                    return true;
                }
                return false;
            }
        });

        builder.setPositiveButton(android.R.string.ok, null);
        builder.setNeutralButton(R.string.help, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(getActivity(), getString(R.string.in_dev), Toast.LENGTH_LONG).show();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dismiss();
            }
        });

        view.clearFocus();
        alert = builder.create();
        alert.setCanceledOnTouchOutside(false);
        alert.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button b = alert.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean valid = inscriptionView.checkField();
                        if (valid) {
                            mFieldLayout.setVisibility(View.GONE);
                            mProgressLayout.setVisibility(View.VISIBLE);
                            alert.getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.GONE);
                            alert.getButton(AlertDialog.BUTTON_NEUTRAL).setVisibility(View.GONE);
                            alert.getButton(AlertDialog.BUTTON_NEGATIVE).setVisibility(View.GONE);
                            mConfirmPassword.requestFocus();
                            Application.hideKeyboard(getActivity(), mConfirmPassword);
                            if(currency == null) {
                                selectorCurrencyView.checkCurrency(new CallbackBoolean() {
                                    @Override
                                    public void methode(boolean noError) {
                                        if (!noError) {
                                            mFieldLayout.setVisibility(View.VISIBLE);
                                            mProgressLayout.setVisibility(View.GONE);
                                            alert.getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.VISIBLE);
                                            alert.getButton(AlertDialog.BUTTON_NEUTRAL).setVisibility(View.VISIBLE);
                                            alert.getButton(AlertDialog.BUTTON_NEGATIVE).setVisibility(View.VISIBLE);
                                        }
                                    }
                                });
                            }else{
                                inscriptionView.validateWallet(currency, getFragmentManager());
                            }
                        }
                    }
                });
            }
        });
        return alert;
    }

    public void badPublicKey(){
        mFieldLayout.setVisibility(View.VISIBLE);
        mProgressLayout.setVisibility(View.GONE);
        alert.getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.VISIBLE);
        alert.getButton(AlertDialog.BUTTON_NEUTRAL).setVisibility(View.VISIBLE);
        alert.getButton(AlertDialog.BUTTON_NEGATIVE).setVisibility(View.VISIBLE);
        mSalt.setError(getString(R.string.wallet_already_registered));
        mSalt.selectAll();
        mSalt.requestFocus();
    }

    public void badUid(){
        mFieldLayout.setVisibility(View.VISIBLE);
        mProgressLayout.setVisibility(View.GONE);
        alert.getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.VISIBLE);
        alert.getButton(AlertDialog.BUTTON_NEUTRAL).setVisibility(View.VISIBLE);
        alert.getButton(AlertDialog.BUTTON_NEGATIVE).setVisibility(View.VISIBLE);
        mUid.setError(getString(R.string.uid_alredy_exist));
        mUid.selectAll();
        mUid.requestFocus();
    }
/*
    @Override
    public void onErrorResponse(VolleyError error) {
        error.printStackTrace();
        if(error == null){
            Toast.makeText(Application.getContext(), getResources().getString(R.string.wallet_already_exists), Toast.LENGTH_SHORT).show();
        }else if(error instanceof NoConnectionError) {
            Toast.makeText(Application.getContext(),
                    getResources().getString(R.string.no_connection),
                    Toast.LENGTH_LONG).show();
        }else {
            Toast.makeText(Application.getContext(), error.toString(), Toast.LENGTH_LONG).show();
        }
        mFieldLayout.setVisibility(View.VISIBLE);
        mProgressLayout.setVisibility(View.GONE);
        alert.getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.VISIBLE);
        alert.getButton(AlertDialog.BUTTON_NEUTRAL).setVisibility(View.VISIBLE);
        alert.getButton(AlertDialog.BUTTON_NEGATIVE).setVisibility(View.VISIBLE);
    }*/
}



