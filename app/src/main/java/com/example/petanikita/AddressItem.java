package com.example.petanikita;

public class AddressItem {
    private int locationId;
    private int provinceId, regencyId, districtId;
    private String provinceName, regencyName, districtName, address;

    private String userName;
    private String userPhone;

    public AddressItem(int locationId, int provinceId, int regencyId, int districtId,
                       String provinceName, String regencyName, String districtName, String address) {
        this.locationId = locationId;
        this.provinceId = provinceId;
        this.regencyId = regencyId;
        this.districtId = districtId;
        this.provinceName = provinceName;
        this.regencyName = regencyName;
        this.districtName = districtName;
        this.address = address;
    }

    public void setUserInfo(String userName, String userPhone) {
        this.userName = userName;
        this.userPhone = userPhone;
    }

    public int getLocationId() { return locationId; }
    public int getProvinceId() { return provinceId; }
    public int getRegencyId() { return regencyId; }
    public int getDistrictId() { return districtId; }
    public String getProvinceName() { return provinceName; }
    public String getRegencyName() { return regencyName; }
    public String getDistrictName() { return districtName; }
    public String getAddress() { return address; }
    public String getUserName() { return userName; }
    public String getUserPhone() { return userPhone; }
}