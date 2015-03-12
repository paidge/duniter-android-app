package io.ucoin.app.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import io.ucoin.app.technical.ObjectUtils;

/**
 * A wallet is a user account
 * Created by eis on 13/01/15.
 */
public class Contact implements LocalEntity, Serializable {

    private Long id;
    private Long accountId;
    private String name;
    private List<Identity> identities = new ArrayList<Identity>();

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public List<Identity> getIdentities() {
        return identities;
    }

    public void setIdentities(List<Identity> identities) {
        this.identities = identities;
    }

    public void addIdentity(Identity identity) {
        this.identities.add(identity);
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("id=").append(id)
                .append(",accountId=").append(accountId)
                .append(",name=").append(name)
                .toString();
    }

    public void copy(Contact contact) {
        this.id = contact.id;
        this.accountId = contact.accountId;
        this.name = contact.name;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o instanceof  Contact) {
            Contact bi = (Contact)o;
            return  ObjectUtils.equals(this.id, bi.id)
                    && ObjectUtils.equals(this.accountId, bi.accountId)
                    && ObjectUtils.equals(this.name, bi.name);
        }
        return false;
    }
}