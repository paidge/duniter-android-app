package org.duniter.app.model.EntityWeb;

import android.content.Context;

import org.duniter.app.model.Entity.Currency;
import org.duniter.app.services.WebService;

/**
 * Created by naivalf27 on 19/04/16.
 */
public class LookupWeb extends Web{
    private Currency currency;
    private String search;

    public LookupWeb(Context context, Currency currency, String search) {
        super(context);
        this.currency = currency;
        this.search = search;
    }

    @Override
    public String getUrl() {
        return "http://" + WebService.getServeur(context,currency) + "/wot/lookup/" + search;
    }

    @Override
    public String postUrl() {
        return null;
    }
}
