package androks.washerapp.Models;

/**
 * Created by androks on 12/1/2016.
 */

public class Car {
    private String id;
    private String uid;
    private String model;
    private String type;
    private String mark;

    public  Car (){}
    public Car(String id, String uid, String model, String type, String mark) {
        this.id = id;
        this.uid = uid;
        this.model = model;
        this.type = type;
        this.mark = mark;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setMark(String mark) {
        this.mark = mark;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getId() {
        return id;
    }

    public String getMark() {
        return mark;
    }

    public String getType() {
        return type;
    }

    public String getModel() {
        return model;
    }

    public String getUid() {
        return uid;
    }
}
