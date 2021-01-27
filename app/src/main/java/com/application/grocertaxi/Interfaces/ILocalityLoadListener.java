package com.application.grocertaxi.Interfaces;

import com.application.grocertaxi.Model.Locality;

import java.util.List;

public interface ILocalityLoadListener {
    void onLocalityLoadSuccess(List<Locality> locality);
    void onLocalityLoadFailed(String message);
}
