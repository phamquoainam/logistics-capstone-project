package com.elogistic.backend.dispatch.api;


public interface AdminNetworkFacts {
    interface Access { void requireNetworkFacts(); } String availability(Access actor); }
