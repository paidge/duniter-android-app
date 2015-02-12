package io.ucoin.app.fragment;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import io.ucoin.app.R;
import io.ucoin.app.activity.MainActivity;
import io.ucoin.app.adapter.ProgressViewAdapter;
import io.ucoin.app.adapter.WalletArrayAdapter;
import io.ucoin.app.model.BlockchainParameter;
import io.ucoin.app.model.Identity;
import io.ucoin.app.model.Wallet;
import io.ucoin.app.service.ServiceLocator;
import io.ucoin.app.service.exception.InsufficientCreditException;
import io.ucoin.app.service.remote.TransactionRemoteService;
import io.ucoin.app.technical.AsyncTaskHandleException;
import io.ucoin.app.technical.ObjectUtils;

public class TransferFragment extends Fragment {

    private TextView mReceiverUidView;
    private Spinner  mWalletSpinner;
    private WalletArrayAdapter mWalletAdapter;
    private EditText mAmountText;
    private TextView mConvertedText;
    private TextView mAmountUnitText;
    private TextView mConvertedUnitText;
    private EditText mCommentText;
    private Button mSendButton;
    private ProgressViewAdapter mProgressViewAdapter;

    private boolean mIsCoinUnit = true;
    private Integer mUniversalDividend = null;
    private boolean mIsRunningConvertion = false;

    private Identity mReceiverIdentity;

    private TransferTask mTransferTask = null;

    public static TransferFragment newInstance(Identity identity) {
        TransferFragment fragment = new TransferFragment();
        Bundle args = new Bundle();
        args.putSerializable(Identity.class.getSimpleName(), identity);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        final List<Wallet> wallets = ServiceLocator.instance().getWalletService().getWallets(getActivity().getApplication());
        mWalletAdapter = new WalletArrayAdapter(
                getActivity(),
                android.R.layout.simple_spinner_item,
                wallets
        );
        mWalletAdapter.setDropDownViewResource(R.layout.list_item_wallet);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        return inflater.inflate(R.layout.fragment_transfer,
                container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //getActivity().setTitle(R.string.transfer);

        Bundle newInstanceArgs = getArguments();
        mReceiverIdentity = (Identity) newInstanceArgs
                .getSerializable(Identity.class.getSimpleName());

        // Source wallet
        mWalletSpinner = ((Spinner) view.findViewById(R.id.wallet));
        mWalletSpinner.setAdapter(mWalletAdapter);

        // target uid
        ((TextView) view.findViewById(R.id.receiver_uid)).setText(mReceiverIdentity.getUid());

        // Amount
        mAmountText = (EditText)view.findViewById(R.id.amount);
        mAmountText.addTextChangedListener(new TextWatcher() {
           @Override
           public void beforeTextChanged(CharSequence s, int start, int count, int after) {
           }

           @Override
           public void onTextChanged(CharSequence s, int start, int before, int count) {
               updateComvertedAmountView(mIsCoinUnit);
           }

           @Override
           public void afterTextChanged(Editable s) {
           }
        });
        mAmountText.requestFocus();

        // Unit
        mAmountUnitText = (TextView)view.findViewById(R.id.amount_unit_text);

        // Converted amount
        mConvertedText = (TextView)view.findViewById(R.id.converted_amount);

        // Converted unit
        mConvertedUnitText = (TextView)view.findViewById(R.id.converted_amount_unit_text);

        // Toggle unit image
        ImageView toggleImage = (ImageView)view.findViewById(R.id.toggle_unit_button);
        toggleImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleUnits();
            }
        });

        // Comment
        mCommentText = (EditText)view.findViewById(R.id.comment);
        mCommentText.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId,
                                          KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    return attemptTransfer();
                }
                return false;
            }
        });

        // Transfer button
        mSendButton = (Button)view.findViewById(R.id.send_button);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptTransfer();
            }
        });

        // progress view
        mProgressViewAdapter = new ProgressViewAdapter(
                view.findViewById(R.id.transfer_progress),
                mSendButton);

        // Load data need for transfer
        loadDataTask();
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.toolbar_transfer, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        Bundle newInstanceArgs = getArguments();
        final Identity identity = (Identity) newInstanceArgs
                .getSerializable(Identity.class.getSimpleName());
        getActivity().setTitle(identity.getUid());
        ((MainActivity)getActivity()).setBackButtonEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    /* -- Internal methods -- */

    protected void loadDataTask() {
        LoadDataTask loadDataTask = new LoadDataTask();
        loadDataTask.execute((Void) null);
    }

    protected boolean attemptTransfer() {

        // Reset errors.
        mAmountText.setError(null);
        mCommentText.setError(null);

        // Store values
        String amountStr = mAmountText.getText().toString();
        // TODO : get from converted is unit has been inversed

        boolean cancel = false;
        View focusView = null;
        Wallet wallet = (Wallet)mWalletSpinner.getSelectedItem();
        // Check wallet selected
        if (wallet == null) {
            mWalletAdapter.setError(mWalletSpinner.getSelectedView(), getString(R.string.field_required));
            focusView = mWalletSpinner;
            cancel = true;
        }

        // Check for a valid uid
        if (TextUtils.isEmpty(amountStr)) {
            mAmountText.setError(getString(R.string.field_required));
            focusView = mAmountText;
            cancel = true;
        } else if (!isAmountValid(amountStr)) {
            mAmountText.setError(getString(R.string.amount_not_integer));
            focusView = mAmountText;
            cancel = true;
        } else {
            if (mIsCoinUnit) {
                amountStr = mAmountText.getText().toString();
            }
            else {
                amountStr = mConvertedText.getText().toString();
            }
            if (wallet.getCredit() < Long.parseLong(amountStr)) {
                mWalletAdapter.setError(mWalletSpinner.getSelectedView(), getString(R.string.insufficient_credit));
                focusView = mWalletSpinner;
                cancel = true;
            }
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
            return false;
        } else {
            doTransfert(wallet);
            return true;
        }
    }

    protected void doTransfert(final Wallet wallet) {
        // If user is authenticate on wallet : perform the transfer
        if (wallet.isAuthenticate()) {
            mTransferTask = new TransferTask();
            mTransferTask.execute(wallet);
        }
        else {
            // Second step: add currency
            LoginFragment fragment = LoginFragment.newInstance(wallet, new LoginFragment.OnClickListener() {
                public void onPositiveClick(Bundle bundle) {
                    Wallet authWallet = (Wallet)bundle.getSerializable(Wallet.class.getSimpleName());
                    // Make sure this is the same wallet returned
                    if (ObjectUtils.equals(authWallet.getPubKeyHash(), wallet.getPubKeyHash())) {
                        getFragmentManager().popBackStack(); // back to transfer fragment
                        doTransfert(authWallet);
                    }
                    else {
                        // TODO : show a message: wrong password ?
                    }
                }
            });
            getFragmentManager().beginTransaction()
                    .setCustomAnimations(
                            R.animator.delayed_fade_in,
                            R.animator.fade_out,
                            R.animator.delayed_fade_in,
                            R.animator.fade_out)
                    .replace(R.id.frame_content, fragment, fragment.getClass().getSimpleName())
                    .addToBackStack(fragment.getClass().getSimpleName())
                    .commit();
        }
    }

    protected boolean isAmountValid(String amountStr) {
        try {
            Long.parseLong(amountStr);
            return true;
        } catch(NumberFormatException e) {
            return false;
        }
    }

    protected void toggleUnits() {
        mIsCoinUnit = !mIsCoinUnit;
        CharSequence amountUnitText = mAmountUnitText.getText();
        mAmountUnitText.setText(mConvertedUnitText.getText());
        mConvertedUnitText.setText(amountUnitText);
        updateComvertedAmountView(mIsCoinUnit);
    }

    protected void updateComvertedAmountView(boolean isCoinUnit) {
        // If data not loaded: do nothing
        if (mUniversalDividend == null || mIsRunningConvertion) {
            return;
        }
        mIsRunningConvertion = true;

        String amountStr = mAmountText.getText().toString();
        if (TextUtils.isEmpty(amountStr)) {
            mConvertedText.setText("");
        }
        else {
            double amount = Double.parseDouble(amountStr);

            double convertedAmount;
            // if amount unit = coins
            if (isCoinUnit) {
                convertedAmount = amount / mUniversalDividend;
            }
            // if amount unit = UD
            else {
                convertedAmount = amount * mUniversalDividend;
            }

            mConvertedText.setText(Double.toString(convertedAmount));

        }
        mIsRunningConvertion = false;
    }


    public class LoadDataTask extends AsyncTaskHandleException<Void, Void, Boolean>{
        @Override
        protected Boolean doInBackgroundHandleException(Void... strings) {
            BlockchainParameter p = ServiceLocator.instance().getDataContext().getBlockchainParameter();
            if (p == null) {
                p = ServiceLocator.instance().getBlockchainRemoteService().getParameters();
            }

            if (p == null) {
                return false;
            }

            // load UD
            mUniversalDividend = p.getUd0();

            // TODO: load other variables


            return true;
        }

        @Override
        protected void onSuccess(Boolean success) {
            if (success == null || !success.booleanValue()) {
                Toast.makeText(getActivity(),
                        "Could not load data. Blockchain parameter not loaded.",
                        Toast.LENGTH_SHORT).show();
            }
            else {

                mSendButton.setEnabled(success);
            }
        }
    }

    public class TransferTask extends AsyncTaskHandleException<Wallet, Void, Boolean>{

        @Override
        protected void onPreExecute() {
            // Hide the keyboard, in case we come from imeDone)
            InputMethodManager inputManager = (InputMethodManager)
                    getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow((null == getActivity().getCurrentFocus())
                            ? null
                            : getActivity().getCurrentFocus().getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);

            // Show the progress bar
            mProgressViewAdapter.showProgress(true);
        }

        @Override
        protected Boolean doInBackgroundHandleException(Wallet... wallets) throws Exception {
            TransactionRemoteService txService = ServiceLocator.instance().getTransactionRemoteService();

            CharSequence amountStr;
            if (mIsCoinUnit) {
                amountStr = mAmountText.getText();
            }
            else {
                amountStr = mConvertedText.getText();
            }
            long amount = Long.parseLong(amountStr.toString());

            txService.transfert(
                    wallets[0],
                    mReceiverIdentity.getPubkey(),
                    amount,
                    mCommentText.getText().toString()
                    );

            return true;
        }

        @Override
        protected void onSuccess(Boolean success) {
            mProgressViewAdapter.showProgress(false);
            if (success == null || !success.booleanValue()) {
                Toast.makeText(getActivity(),
                        getString(R.string.transfer_error),
                        Toast.LENGTH_SHORT).show();
            }
            else {
                getFragmentManager().popBackStack(); // return back

                Toast.makeText(getActivity(),
                        getString(R.string.transfer_sended),
                        Toast.LENGTH_LONG).show();
                // TODO smoul : could you go back to previous fragment ?
                // Or maybe to a new transaction history fragment ?
            }
        }

        @Override
        protected void onFailed(Throwable error) {
            super.onFailed(error);
            if (error instanceof InsufficientCreditException) {
                mAmountText.setError("Not enough money in wallet.");
            }
            else {
                Toast.makeText(getActivity(),
                        "Could not send transaction: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }

            mProgressViewAdapter.showProgress(false);
        }
    }


}
