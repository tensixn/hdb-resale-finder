package com.abcde.hdbresale.domain;

import jakarta.persistence.*;

/** Entity: one HDB resale transaction from data.gov.sg (Data Dictionary: Transaction). */
@Entity
@Table(name = "resale_transaction")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "flat_id")
    private Long flatId;
    @Column(name = "txn_month")
    private String month;
    @Column(name = "resale_price")
    private long resalePrice;
    private String source;

    protected Transaction() {
    }

    public Transaction(Long flatId, String month, long resalePrice, String source) {
        this.flatId = flatId;
        this.month = month;
        this.resalePrice = resalePrice;
        this.source = source;
    }

    public Long getId() { return id; }
    public Long getFlatId() { return flatId; }
    public String getMonth() { return month; }
    public long getResalePrice() { return resalePrice; }
    public String getSource() { return source; }
}
