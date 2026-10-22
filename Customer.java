
package com.electricity;

public class Customer {
    public int id;
    public String name;
    public String meterNumber;
    public String address;
    public String phone;
    public String email;

    public Customer(int id, String name, String meterNumber,
                    String address, String phone, String email) {
        this.id = id;
        this.name = name;
        this.meterNumber = meterNumber;
        this.address = address;
        this.phone = phone;
        this.email = email;
    }
}
