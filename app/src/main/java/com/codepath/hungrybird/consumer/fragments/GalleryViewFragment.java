package com.codepath.hungrybird.consumer.fragments;


import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.codepath.hungrybird.R;
import com.codepath.hungrybird.consumer.adapters.GallerySnapListContainerAdapter;
import com.codepath.hungrybird.databinding.ConsumerGalleryViewBinding;
import com.codepath.hungrybird.model.Dish;
import com.codepath.hungrybird.model.DishList;
import com.codepath.hungrybird.network.ParseClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class GalleryViewFragment extends Fragment {
    public static final String TAG = GalleryViewFragment.class.getSimpleName();

    ConsumerGalleryViewBinding binding;
    Map<String, List<Dish>> cuisine2Dishes = new HashMap<>();
    List<Dish.Cuisine> allCuisines;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        allCuisines = Arrays.asList(Dish.Cuisine.values());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.consumer_gallery_view, container, false);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        binding.recyclerView.setHasFixedSize(true);
        loadAllTopCuisineDishes();
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("Hungry's Nest");
    }

    private void setupAdapter(Map<String, List<Dish>> map) {
        GallerySnapListContainerAdapter gallerySnapListContainerAdapter = new GallerySnapListContainerAdapter(getActivity());
        for (String key : map.keySet()) {
            List<Dish> dishes = map.get(key);
            if (dishes != null && !dishes.isEmpty()) {
                gallerySnapListContainerAdapter.addSnap(new DishList(Gravity.CENTER_HORIZONTAL, key, dishes));
            }

        }
        binding.recyclerView.setAdapter(gallerySnapListContainerAdapter);
    }

    private void loadAllTopCuisineDishes() {
        Observable.create(new Observable.OnSubscribe<List<Dish>>() {
            @Override
            public void call(Subscriber<? super List<Dish>> subscriber) {
                ParseClient.getInstance().getDishesByCuisines(allCuisines, new ParseClient.DishListListener() {
                    @Override
                    public void onSuccess(List<Dish> dishes) {
                        subscriber.onNext(dishes);
                        subscriber.onCompleted();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        subscriber.onError(e);
                    }
                });
            }
        }).flatMap(new Func1<List<Dish>, Observable<Map<String, List<Dish>>>>() {
            @Override
            public Observable<Map<String, List<Dish>>> call(List<Dish> dishes) {
                Map<String, List<Dish>> map = new HashMap<String, List<Dish>>();
                for (Dish d : dishes) {
                    List<Dish> list = map.get(d.getCuisine());
                    if (list == null) {
                        list = new ArrayList<>();
                        map.put(d.getCuisine(), list);
                    }
                    list.add(d);
                }
                return Observable.just(map);

            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Map<String, List<Dish>>>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Map<String, List<Dish>> map) {
                        setupAdapter(map);
                    }
                });
    }

}
