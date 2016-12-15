package androks.washerapp.Models;

/**
 * Created by androks on 12/1/2016.
 */

public class Car {
    private String id;

    private String model;
    private String maker;
    private String serialNumber;

    public  Car (){}
    public Car(String maker, String model, String serialNum) {
        this.model = model;
        this.maker = maker;
        this.serialNumber = serialNum;
    }

    public void setMaker(String maker) {
        this.maker = maker;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getMaker() {
        return maker;
    }

    public String getModel() {
        return model;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return getMaker() + " " + getModel() + "\n" + getSerialNumber();
    }
}
