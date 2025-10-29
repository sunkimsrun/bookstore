package com.example.myapplication.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.myapplication.R;
import com.example.myapplication.databinding.FragmentPrivacyPolicyBinding;

public class PrivacyPolicyFragment extends Fragment {

    private FragmentPrivacyPolicyBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_privacy_policy, container, false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public PrivacyPolicyFragment() {
        // Required empty public constructor
    }
}

