package androks.washerapp.Models;

/**
 * Created by androks on 1/22/2017.
 */

public class Review {
    private String id;
    private String uid;
    private String washerId;
    private String text;
    private String date;

    Review(){}

    public Review(String id, String uid, String washerId, String text, String date) {
        this.id = id;
        this.uid = uid;
        this.washerId = washerId;
        this.text = text;
        this.date = date;
    }

    public String getId() {
        return id;
    }

    public String getUid() {
        return uid;
    }

    public String getWasherId() {
        return washerId;
    }

    public String getText() {
        return text;
    }

    public String getDate() {
        return date;
    }
}
