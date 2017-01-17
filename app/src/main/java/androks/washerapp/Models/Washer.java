package androks.washerapp.Models;

/**
 * Created by androks on 11/17/2016.
 */

public class Washer {
    private double langtitude;
    private double longtitude;
    private int standartPrice;
    private int boxes;
    private int freeBoxes;
    private boolean status;
    private boolean cafe;
    private boolean wifi;
    private boolean restRoom;
    private boolean lunchRoom;
    private String hours;
    private String id;
    private String name;
    private String phone;
    private String uid;
    private String location;

    public int getStandartPrice() {
        return standartPrice;
    }

    public boolean getLunchRoom() {
        return lunchRoom;
    }

    public int getBoxes() {
        return boxes;
    }

    public int getFreeBoxes() {
        return freeBoxes;
    }

    public boolean getRestRoom() {
        return restRoom;
    }

    public String getLocation() {
        return location;
    }

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

    public boolean getCafe() {
        return cafe;
    }

    public boolean getWifi() {
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

    public Washer(double langtitude, double longtitude, int standartPrice, int boxes, int freeBoxes, boolean status, boolean cafe, boolean wifi, boolean restRoom, boolean lunchRoom, String hours, String id, String name, String phone, String uid, String location) {
        this.langtitude = langtitude;
        this.longtitude = longtitude;
        this.standartPrice = standartPrice;
        this.boxes = boxes;
        this.freeBoxes = freeBoxes;
        this.status = status;
        this.cafe = cafe;
        this.wifi = wifi;
        this.restRoom = restRoom;
        this.lunchRoom = lunchRoom;
        this.hours = hours;
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.uid = uid;
        this.location = location;
    }
}
