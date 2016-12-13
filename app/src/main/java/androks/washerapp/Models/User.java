package androks.washerapp.Models;

/**
 * Created by androks on 12/10/2016.
 */

public class User {
    private String mEmail;
    private String mPhone;

    public User(){}

    public User(String mEmail) {
        this.mEmail = mEmail;
    }

    public User(String mEmail, String mPhone) {
        this.mEmail = mEmail;
        this.mPhone = mPhone;
    }

    public String getEmail() {
        return mEmail;
    }

    public void setEmail(String mEmail) {
        this.mEmail = mEmail;
    }

    public String getPhone() {
        return mPhone;
    }

    public void setPhone(String mPhone) {
        this.mPhone = mPhone;
    }

}
