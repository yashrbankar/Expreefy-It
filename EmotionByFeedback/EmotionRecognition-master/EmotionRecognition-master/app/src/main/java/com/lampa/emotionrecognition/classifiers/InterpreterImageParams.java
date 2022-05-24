package com.lampa.emotionrecognition.classifiers;

import org.tensorflow.lite.Interpreter;
// A Interpreter encapsulates a pre-trained TensorFlow Lite model,
// in which operations are executed for model inference.
public class InterpreterImageParams 
{
    // Indices of an input image parameters set as 1 2 3
    private final static int MODEL_INPUT_WIDTH_INDEX = 1;
    private final static int MODEL_INPUT_HEIGHT_INDEX = 2;
    private final static int MODEL_INPUT_COLOR_DIM_INDEX = 3;

    // Tensor indices of an image parameters set as 0
    private final static int IMAGE_INPUT_TENSOR_INDEX = 0;
    private final static int IMAGE_OUTPUT_TENSOR_INDEX = 0;

    // Index of an output result array
    private final static int MODEL_OUTPUT_LENGTH_INDEX = 1;
    
    // Image Width Tensorflow
    public static int getInputImageWidth(Interpreter interpreter) 
    {
        return interpreter.getInputTensor(IMAGE_INPUT_TENSOR_INDEX).shape()[MODEL_INPUT_WIDTH_INDEX];
    }

    public static int getInputImageHeight(Interpreter interpreter) 
    {
        return interpreter.getInputTensor(IMAGE_INPUT_TENSOR_INDEX).shape()[MODEL_INPUT_HEIGHT_INDEX];
    }

    public static int getInputColorDimLength(Interpreter interpreter) 
    {
        return interpreter.getInputTensor(IMAGE_INPUT_TENSOR_INDEX).shape()[MODEL_INPUT_COLOR_DIM_INDEX];
    }

    public static int getOutputLength(Interpreter interpreter) 
    {
        return interpreter.getOutputTensor(IMAGE_OUTPUT_TENSOR_INDEX).shape()[MODEL_OUTPUT_LENGTH_INDEX];
    }
}
