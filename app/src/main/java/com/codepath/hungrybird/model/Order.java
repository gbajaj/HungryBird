package com.codepath.hungrybird.model;

import com.parse.ParseClassName;
import com.parse.ParseObject;

/**
 * Created by ajasuja on 4/4/17.
 */
@ParseClassName("Order")
public class Order extends ParseObject {

    public String getOrderName() {
        return getString("orderName");
    }

    public void setOrderName(String orderName) {
        put("orderName", orderName);
    }

    public User getConsumer() {
        return (User) getParseUser("consumer");
    }

    public void setConsumer(User user) {
        put("consumer", user);
    }

    public User getChef() {
        return (User) getParseUser("chef");
    }

    public void setChef(User user) {
        put("chef", user);
    }

    public String getStatus() {
        return getString("status");
    }

    public void setStatus(String status) {
        put("status", status);
    }

    public enum Status {
        NOT_ORDERED("NOT_ORDERED"),
        ORDERED("ORDERED"),
        IN_PROGRESS("IN_PROGRESS"),
        READY_FOR_PICKUP("READY_FOR_PICKUP"),
        OUT_FOR_DELIVERY("OUT_FOR_DELIVERY"),
        DONE("DONE"),
        CANCELLED("CANCELLED");

        private String statusValue;

        Status(String statusValue) {
            this.statusValue = statusValue;
        }

        public String getStatusValue() {
            return this.statusValue;
        }
    }
}