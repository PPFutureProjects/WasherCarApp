package androks.washerapp.Models;

/**
 * Created by androks on 11/17/2016.
 */

public class Washer {
    private boolean status;
    private double langtitude;
    private double longtitude;
    private boolean cafe;
    private boolean wifi;
    private String hours;
    private String id;
    private String name;
    private String phone;
    private String uid;

    public void setStatus(boolean status) {
        this.status = status;
    }

    public boolean getStatus() {
        return status;
    }

    public double getLangtitude() {
        return langtitude;
    }

    public double getLongtitude() {
        return longtitude;
    }

    public boolean isCafe() {
        return cafe;
    }

    public boolean isWifi() {
        return wifi;
    }

    public String getHours() {
        return hours;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getUid() {
        return uid;
    }

    Washer(){}

    public Washer(double langtitude, double longtitude, String id){
        this.langtitude = langtitude;
        this.longtitude = longtitude;
        this.id = id;
    }

    public Washer(double langtitude, double longtitude, boolean cafe, boolean wifi, String hours, String id, String name, String phone, String uid) {
        this(langtitude, longtitude, id);
        this.cafe = cafe;
        this.wifi = wifi;
        this.hours = hours;
        this.name = name;
        this.phone = phone;
        this.uid = uid;
    }
}
