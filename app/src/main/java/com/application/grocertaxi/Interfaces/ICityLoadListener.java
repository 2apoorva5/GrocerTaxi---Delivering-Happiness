package com.application.grocertaxi.Interfaces;

import com.application.grocertaxi.Model.City;

import java.util.List;

public interface ICityLoadListener {
    void onCityLoadSuccess(List<City> city);
    void onCityLoadFailed(String message);
}
