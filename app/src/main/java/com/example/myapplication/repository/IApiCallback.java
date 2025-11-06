package com.example.myapplication.repository;

public interface IApiCallback<T> {

    void onSuccess(T result);

    void onError(String errorMessage);
}
