package com.example.livenessdetectionfinalyearproject.mlOps.model

class ModelInfo(
    val name : String ,
    val assetsFilename : String ,
    val cosineThreshold : Float ,
    val l2Threshold : Float ,
    val outputDims : Int ,
    val inputDims : Int ) {

}