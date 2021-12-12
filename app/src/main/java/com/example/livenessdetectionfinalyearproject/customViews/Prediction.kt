package com.example.livenessdetectionfinalyearproject.customViews

import android.graphics.Rect

data class Prediction(var bbox : Rect, var label : String, var maskLabel : String = "" )