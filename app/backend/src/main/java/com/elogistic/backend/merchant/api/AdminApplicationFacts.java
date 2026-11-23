package com.elogistic.backend.merchant.api;


public interface AdminApplicationFacts {
    interface Access { void requireApplicationFacts(); } String availability(Access actor); }
