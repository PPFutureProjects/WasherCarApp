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
    private String hours;
    private String id;
    private String name;
    private String phone;
    private String uid;
    private String location;

    public int getStandartPrice() {
        return standartPrice;
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

    public Washer(boolean status, String location, String uid, String phone, String name, String id, String hours, boolean wifi, boolean restRoom, boolean cafe, int freeBoxes, int boxes, int standartPrice, double longtitude, double langtitude) {
        this.status = status;
        this.location = location;
        this.uid = uid;
        this.phone = phone;
        this.name = name;
        this.id = id;
        this.hours = hours;
        this.wifi = wifi;
        this.restRoom = restRoom;
        this.cafe = cafe;
        this.freeBoxes = freeBoxes;
        this.boxes = boxes;
        this.standartPrice = standartPrice;
        this.longtitude = longtitude;
        this.langtitude = langtitude;
    }
}
