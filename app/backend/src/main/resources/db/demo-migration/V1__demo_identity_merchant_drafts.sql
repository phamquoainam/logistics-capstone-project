CREATE SCHEMA IF NOT EXISTS identity;
CREATE SCHEMA IF NOT EXISTS merchant;
CREATE SCHEMA IF NOT EXISTS orders;
CREATE TABLE merchant.demo_merchant (id UUID PRIMARY KEY, display_name VARCHAR(160) NOT NULL, reference VARCHAR(80) NOT NULL UNIQUE);
CREATE TABLE identity.demo_account (
 id UUID PRIMARY KEY, username VARCHAR(80) NOT NULL UNIQUE, password_hash VARCHAR(100) NOT NULL,
 display_name VARCHAR(160) NOT NULL, role VARCHAR(30) NOT NULL CHECK (role IN ('MERCHANT','ADMIN','DISPATCHER','WAREHOUSE_OPERATOR','SHIPPER','FINANCE')),
 enabled BOOLEAN NOT NULL, merchant_id UUID,
 CHECK ((role='MERCHANT' AND merchant_id IS NOT NULL) OR (role<>'MERCHANT' AND merchant_id IS NULL))
);
CREATE TABLE orders.demo_shipment (
 id UUID PRIMARY KEY, merchant_id UUID NOT NULL, reference VARCHAR(80) NOT NULL,
 pickup_address VARCHAR(500) NOT NULL, recipient_name VARCHAR(160) NOT NULL, recipient_phone VARCHAR(30) NOT NULL,
 delivery_street VARCHAR(300) NOT NULL, delivery_ward VARCHAR(160) NOT NULL, delivery_province VARCHAR(160) NOT NULL,
 parcel_description VARCHAR(300) NOT NULL, weight_kg DECIMAL(12,3) NOT NULL CHECK (weight_kg>0 AND weight_kg<=1000),
 status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT','CANCELLED')), created_by UUID NOT NULL,
 created_at TIMESTAMP WITH TIME ZONE NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
 UNIQUE(merchant_id,reference)
);
CREATE INDEX idx_demo_shipment_owner_created ON orders.demo_shipment(merchant_id,created_at);
CREATE TABLE orders.demo_event (
 id UUID PRIMARY KEY, shipment_id UUID NOT NULL REFERENCES orders.demo_shipment(id),
 event_type VARCHAR(30) NOT NULL CHECK(event_type IN ('DRAFT_CREATED','DRAFT_CANCELLED')),
 actor_id UUID NOT NULL, reason VARCHAR(300), occurred_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE orders.demo_operation (
 merchant_id UUID NOT NULL, operation VARCHAR(120) NOT NULL, operation_key VARCHAR(100) NOT NULL,
 payload_hash VARCHAR(64) NOT NULL, shipment_id UUID NOT NULL REFERENCES orders.demo_shipment(id),
 PRIMARY KEY(merchant_id,operation,operation_key)
);
