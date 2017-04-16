package com.codepath.hungrybird.consumer.fragments;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.bumptech.glide.Glide;
import com.codepath.hungrybird.R;
import com.codepath.hungrybird.common.BaseItemHolderAdapter;
import com.codepath.hungrybird.common.DateUtils;
import com.codepath.hungrybird.common.StringsUtils;
import com.codepath.hungrybird.databinding.ConsumerOrderDetailsDishItemBinding;
import com.codepath.hungrybird.databinding.ConsumerOrderDetailsFragmentBinding;
import com.codepath.hungrybird.model.Dish;
import com.codepath.hungrybird.model.Order;
import com.codepath.hungrybird.model.OrderDishRelation;
import com.codepath.hungrybird.model.User;
import com.codepath.hungrybird.network.ParseClient;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by gauravb on 4/13/17.
 */

public class OrderDetailsFragment extends Fragment {
    public static final String OBJECT_ID = "OBJECT_ID";
    public static final String IS_CHEF = "IS_CHEF";
    ConsumerOrderDetailsFragmentBinding binding;
    ArrayList<OrderDishRelation> orderDishRelations = new ArrayList<>();
    DateUtils dateUtils = new DateUtils();
    StringsUtils stringsUtils = new StringsUtils();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = DataBindingUtil.inflate(inflater, R.layout.consumer_order_details_fragment, container, false);
        Bundle bundle = getArguments();
        String orderObjectId = bundle.getString(OBJECT_ID);
        final boolean isChef = new User(ParseUser.getCurrentUser()).isChef();
        ParseClient parseClient = ParseClient.getInstance();

        final BaseItemHolderAdapter<OrderDishRelation> adapter =
                new BaseItemHolderAdapter(getContext(), R.layout.consumer_order_details_dish_item, orderDishRelations);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        binding.consumerOrderDetailsRv.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(binding.consumerOrderDetailsRv.getContext(),
                linearLayoutManager.getOrientation());
        binding.consumerOrderDetailsRv.addItemDecoration(dividerItemDecoration);
        binding.consumerOrderDetailsRv.setAdapter(adapter);
        // Create an ArrayAdapter using the string array and a default spinner
        if (isChef) {
            ArrayAdapter<CharSequence> staticAdapter = ArrayAdapter
                    .createFromResource(getContext(), R.array.order_status_array,
                            android.R.layout.simple_spinner_item);
            binding.orderStatusSpinner.setVisibility(View.VISIBLE);
            binding.orderStatusSpinner.setAdapter(staticAdapter);
            binding.consumerOrderStatus.setVisibility(View.GONE);
        }

        parseClient.getOrderDishRelationsByOrderId(orderObjectId, new ParseClient.OrderDishRelationListListener() {
            @Override
            public void onSuccess(List<OrderDishRelation> orderDishRelations) {

                if (orderDishRelations.isEmpty() == false) {
                    Order order = orderDishRelations.get(0).getOrder();
                    if (order != null) {
                        if (isChef) {
                            setSpinnerToValue(binding.orderStatusSpinner, order);
                        }

                        try {
                            Date d = order.getCreatedAt();
                            String date = dateUtils.getDate(d);
                            binding.consumerOrderDateTv.setText(date);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        binding.consumerOrderCode.setText(order.getDisplayId());
                        binding.consumerOrderStatus.setText(stringsUtils.displayStatusString(order));
                        binding.paymentType.setText(order.getPaymentType());
                        binding.deliveryAddress.setText(order.getDeliveryAddress());

                    }
                    double totalTax = 0.0;
                    double totalPriceBeforeTax = 0.0;
                    Double shippingAndService = order.getShippingFee();
                    for (OrderDishRelation o : orderDishRelations) {
                        totalTax += o.getQuantity() * o.getTaxPerItem();
                        totalPriceBeforeTax += o.getQuantity() * o.gePricePerItem();
                    }
                    double subtotal = totalTax + totalPriceBeforeTax;
                    double finalTotal = subtotal + shippingAndService;

                    binding.itemsTotalValue.setText("$" + totalPriceBeforeTax);
                    binding.itemsTotalTaxValue.setText("$" + Math.round(100 * totalTax) / 100.0);
                    String shipping = "FREE";
                    if (new Double(0.0).compareTo(shippingAndService) != 0) {
                        shipping = "$" + Math.round(100 * shippingAndService) / 100.0;
                    }
                    binding.shippingServiceValue.setText(shipping);
                    binding.itemsTotalSubtotalValue.setText("$" + subtotal);
                    binding.finalTotal.setText("$" + finalTotal);
                }

                OrderDetailsFragment.this.orderDishRelations.addAll(orderDishRelations);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Exception e) {

            }
        });

        adapter.setViewBinder((holder, item, position) -> {
            OrderDishRelation order = orderDishRelations.get(position);
            ConsumerOrderDetailsDishItemBinding binding = (ConsumerOrderDetailsDishItemBinding) (holder.binding);
            Dish dish = order.getDish();
            if (dish.getPrimaryImage() != null) {
                Glide.with(getActivity()).load(dish.getPrimaryImage().getUrl()).into(binding.itemImage);
            }

            int count = order.getQuantity();

            Double pricePerItem = order.gePricePerItem();
            double totalPrice = count * pricePerItem;
            binding.consumerOrderDetailQuantityDetailsTv.setText("" + count + " at $" + pricePerItem + " each");
            binding.itemTotalPrice.setText("$" + totalPrice);
            binding.consumerOrderDetailDishNameTv.setText(dish.getDishName());


            //todo: get price from order
            //todo: get quantity and calculate
        });
        return binding.getRoot();
    }

    public void setSpinnerToValue(Spinner spinner, Order order) {
        setSpinnerToValue(spinner, order.getStatus());
        binding.orderStatusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String value = spinner.getSelectedItem().toString();
                Order.Status newStatus = null;
                if (getString(R.string.order_status_received).equals(value)) {
                    newStatus = Order.Status.ORDERED;
                } else if (getString(R.string.order_status_in_progress).equals(value)) {
                    newStatus = Order.Status.IN_PROGRESS;
                } else if (getString(R.string.order_status_ready_for_pickup).equals(value)) {
                    newStatus = Order.Status.READY_FOR_PICKUP;
                } else if (getString(R.string.order_status_out_for_delivery).equals(value)) {
                    newStatus = Order.Status.OUT_FOR_DELIVERY;
                } else if (getString(R.string.order_status_complete).equals(value)) {
                    newStatus = Order.Status.COMPLETE;
                } else if (getString(R.string.order_status_cancel).equals(value)) {
                    newStatus = Order.Status.CANCELLED;
                }
                if (newStatus != null) {
                    order.setStatus(newStatus.name());
                    ParseClient.getInstance().addOrder(order, new ParseClient.OrderListener() {
                        @Override
                        public void onSuccess(Order order) {

                        }

                        @Override
                        public void onFailure(Exception e) {

                        }
                    });
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

    }

    public void setSpinnerToValue(Spinner spinner, String value) {
        int index = 0;
//        SpinnerAdapter adapter = spinner.getAdapter();
//        String setValue = null;
////        <item>@string/order_status_received</item>
////        <item>@string/order_status_in_progress</item>
////        <item>@string/order_status_ready_for_pickup</item>
////        <item>@string/order_status_out_for_delivery</item>
////        <item>@string/order_status_complete</item>
////        <item>@string/order_status_cancel</item>
////        NOT_ORDERED("NOT_ORDERED"),
////                ORDERED("ORDERED"),
////                IN_PROGRESS("IN_PROGRESS"),
////                READY_FOR_PICKUP("READY_FOR_PICKUP"),
////                OUT_FOR_DELIVERY("OUT_FOR_DELIVERY"),
////                COMPLETE("COMPLETE"),
////                CANCELLED("CANCELLED");
        Order.Status status = Order.Status.valueOf(value);
        index = -1;
        switch (status) {
            case ORDERED:
                index = 0;
                break;
            case IN_PROGRESS:
                index = 1;
                break;
            case READY_FOR_PICKUP:
                index = 2;
                break;
            case OUT_FOR_DELIVERY:
                index = 3;
            case COMPLETE:
                index = 4;
                break;
            case CANCELLED:
                index = 5;
                break;
            default:
                index = -1;
        }
        if (index != -1) {
            spinner.setSelection(index);
        }
    }
}