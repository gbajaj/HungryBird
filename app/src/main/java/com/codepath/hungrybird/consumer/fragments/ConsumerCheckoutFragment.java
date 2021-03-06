package com.codepath.hungrybird.consumer.fragments;


import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.codepath.hungrybird.R;
import com.codepath.hungrybird.consumer.activities.GalleryActivity;
import com.codepath.hungrybird.databinding.ConsumerCheckoutFragmentBinding;
import com.codepath.hungrybird.model.Order;
import com.codepath.hungrybird.model.postmates.DeliveryRequest;
import com.codepath.hungrybird.model.postmates.DeliveryResponse;
import com.codepath.hungrybird.network.ParseClient;
import com.codepath.hungrybird.network.PostmatesClient;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.parse.ParseCloud;
import com.stripe.android.Stripe;
import com.stripe.android.TokenCallback;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.HashMap;

import cz.msebera.android.httpclient.Header;

public class ConsumerCheckoutFragment extends Fragment {

    public static final String ORDER_ID = "ORDER_ID";
    public static final String TOTAL_PRICE = "TOTAL_PRICE";

    ConsumerCheckoutFragmentBinding binding;

    CheckoutFragmentListener checkoutFragmentListener;
    private static DecimalFormat df = new DecimalFormat();

    static {
        df.setMinimumFractionDigits(2);
        df.setMaximumFractionDigits(2);
    }

    Order order;

    public interface CheckoutFragmentListener {
        void onPaymentSuccessfully();

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.getItem(0).setVisible(false);
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onResume() {
        super.onResume();
        ((GalleryActivity) getActivity()).setToolbarTitle("Enter Credit Card");
    }

    private String getRoundedTwoPlaces(double val) {
        return "$" + df.format(Math.round(100 * val) / 100.0);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof CheckoutFragmentListener) {
            checkoutFragmentListener = (CheckoutFragmentListener) context;
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement ConsumerCheckoutFragment.CheckoutFragmentListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.consumer_checkout_fragment, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.cartCheckoutPaynowBt.setText("PAY " + getRoundedTwoPlaces(Double.parseDouble(getArguments().getString(TOTAL_PRICE))) + " NOW");

        binding.cartCheckoutPaynowBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onPayNowClickListener();
            }
        });

    }

    private void onPayNowClickListener() {
        String orderId = getArguments().getString(ORDER_ID);
        binding.progressBarCyclic.setVisibility(View.VISIBLE);
        binding.cartCheckoutPaynowBt.setText("");

        ParseClient.getInstance().getOrderById(orderId, new ParseClient.OrderListener() {
            @Override
            public void onSuccess(Order order) {
                ConsumerCheckoutFragment.this.order = order;
            }

            @Override
            public void onFailure(Exception e) {

            }
        });

        String convertedPrice = String.valueOf(Double.parseDouble(getArguments().getString(TOTAL_PRICE)) * 100);
        int totalPriceForStripe = Integer.parseInt(convertedPrice.substring(0, convertedPrice.indexOf(".")));
        Card card = new Card(
                "4242424242424242",
                Integer.parseInt("12"),
                Integer.parseInt("2019"),
                "123");
        if (!card.validateCard()) {
            Toast.makeText(getActivity().getApplicationContext(), "No ", Toast.LENGTH_LONG).show();
        } else {
            Stripe stripe = new Stripe(getActivity().getApplicationContext(), "pk_test_u4lZ9tWVhEoZVKVa6FFN5oei");
            stripe.createToken(
                    card,
                    new TokenCallback() {
                        public void onSuccess(Token token) {
                            // Send token to your server
                            Log.e("STRIPE_TOKEN", token.getId());
                            //Charge: http://api.shahdhwani.com/HungryBird/charge.php
                            AsyncHttpClient client = new AsyncHttpClient();
                            RequestParams params = new RequestParams();
                            params.put("token", token.getId());
                            params.put("chargeVal", totalPriceForStripe);
                            params.put("orderId", orderId);
                            client.post("http://api.shahdhwani.com/HungryBird/charge.php", params, new JsonHttpResponseHandler() {
                                        @Override
                                        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                            try {
                                                String transactionStatusCode = response.getString("response");
                                                Log.e("Success", response.toString() + " " + response.getString("response"));
                                                if ("Success".equals(transactionStatusCode)) {
                                                    if (order.isDelivery()) {
                                                        DeliveryRequest deliveryRequest = deliveryRequest(order.getDeliveryQuoteId());
                                                        PostmatesClient.getInstance().createDelivery(deliveryRequest, new PostmatesClient.DeliveryResponseListener() {
                                                            @Override
                                                            public void onSuccess(DeliveryResponse deliveryResponse) {
                                                                order.setDeliveryId(deliveryResponse.getDeliveryId());
                                                                postStripeSuccess();
                                                            }

                                                            @Override
                                                            public void onFailure(Exception e) {

                                                            }
                                                        });
                                                    } else {
                                                        postStripeSuccess();
                                                    }
                                                } else {
                                                    Toast.makeText(getActivity().getApplicationContext(), "There was an error.", Toast.LENGTH_LONG).show();
                                                }
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }

                                        }

                                        @Override
                                        public void onFailure(int statusCode, Header[] headers, String
                                                responseString, Throwable throwable) {
                                            super.onFailure(statusCode, headers, responseString, throwable);
                                        }
                                    }
                            );
                        }

                        public void onError(Exception error) {
                            // Show localized error message
                            Toast.makeText(getActivity().getApplicationContext(),
                                    error.toString(),
                                    Toast.LENGTH_LONG
                            ).show();
                            binding.cartCheckoutPaynowBt.setText("PAY " + getRoundedTwoPlaces(Double.parseDouble(getArguments().getString(TOTAL_PRICE))) + " NOW");
                        }
                    }
            );
        }
    }

    private void postStripeSuccess() {
        binding.progressBarCyclic.setVisibility(View.GONE);
        binding.cartCheckoutPaynowBt.setText("Thanks for choosing us!");
        // send push notification
        HashMap<String, String> payload = new HashMap<>();
        payload.put("targetUserId", order.getChef().getObjectId());
        payload.put("orderId", order.getObjectId());
        payload.put("fromChef", String.valueOf(false));
        payload.put("title", "Order Received");
        payload.put("text", "A new order has been received from " + order.getConsumer().getUsername());
        ParseCloud.callFunctionInBackground("pushChannelTest", payload);
        // change order status
        ConsumerCheckoutFragment.this.order.setStatus(Order.Status.ORDERED.name());
        ParseClient.getInstance().addOrder(ConsumerCheckoutFragment.this.order, new ParseClient.OrderListener() {
            @Override
            public void onSuccess(Order order) {
                checkoutFragmentListener.onPaymentSuccessfully();
            }

            @Override
            public void onFailure(Exception e) {

            }
        });
    }

    private DeliveryRequest deliveryRequest(String deliveryQuoteId) {
        DeliveryRequest deliveryRequest = new DeliveryRequest();
        deliveryRequest.setQuoteId(deliveryQuoteId);
        deliveryRequest.setManifest("manifest");
        // chef info
        deliveryRequest.setPickUpName("chef's name");
        deliveryRequest.setPickUpPhoneNumber("111-111-1111");
        deliveryRequest.setPickUpAddress("20 McAllister St, San Francisco, CA"); // chef's address
        // consumer info
        deliveryRequest.setDropOffName("consumer's name");
        deliveryRequest.setDropOffPhoneNumber("222-222-2222");
        deliveryRequest.setDropOffAddress("101 Market St, San Francisco, CA"); // consumer's address
        return deliveryRequest;
    }
}
