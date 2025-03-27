package com.example.totowala

import com.google.firebase.Timestamp
import com.google.type.LatLng

data class Fare(
    var amount: Double,
    var transactionId: String,
    var status: String,
    var timestamp: Timestamp
)

data class Journey(
   var from :LatLng,
   var to :LatLng,
   var startedAt:Timestamp,
   var endedAt:Timestamp?,
   var fare:Fare
)


