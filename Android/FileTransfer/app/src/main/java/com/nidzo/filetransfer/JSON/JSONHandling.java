package com.nidzo.filetransfer.JSON;

import com.nidzo.filetransfer.FileTransferException;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;

public class JSONHandling {
    public static String serializeObject(Object obj) throws FileTransferException {
        return serializeToJSONObject(obj).toString();
    }
    private static JSONObject serializeToJSONObject(Object obj) throws FileTransferException {
        JSONObject returnedObject = new JSONObject();
        try
        {
            for(Field field: obj.getClass().getDeclaredFields())
            {
                if (field.isAnnotationPresent(StringAttribute.class)) {
                    StringAttribute annotation = field.getAnnotation(StringAttribute.class);
                    Object value = field.get(obj);
                    if(value!=null) {
                        returnedObject.put(annotation.name(), value);
                    }
                }
                else if (field.isAnnotationPresent(ObjectAttribute.class)){
                    ObjectAttribute annotation = field.getAnnotation(ObjectAttribute.class);
                    if(field.get(obj)!=null) {
                        returnedObject.put(annotation.name(), serializeToJSONObject(field.get(obj)));
                    }
                }
            }
        }
        catch (IllegalAccessException | JSONException error) {
            throw new FileTransferException("JSON Error: "+error.toString()+"->"+error.getMessage());
        }
        return returnedObject;
    }
    public static void deserializeObject(String jsonString, Object target) throws FileTransferException {
        try {
            deserializeJSONObject(new JSONObject(jsonString), target);
        }
        catch (JSONException error) {
            throw new FileTransferException("JSON Error: "+error.toString()+"->"+error.getMessage());
        }
    }

    public static void deserializeJSONObject(JSONObject jsonObject, Object target) throws FileTransferException {
        try
        {
            for(Field field: target.getClass().getDeclaredFields())
            {
                if (field.isAnnotationPresent(StringAttribute.class)) {
                    StringAttribute annotation = field.getAnnotation(StringAttribute.class);
                    if(jsonObject.has(annotation.name()))
                    {
                        String value = jsonObject.getString(annotation.name());
                        field.set(target, value);
                    }
                    else
                    {
                        if(annotation.required()) throw new JSONException("Missing attribute "+annotation.name());
                        field.set(target, null);
                    }
                }
                else if (field.isAnnotationPresent(ObjectAttribute.class)){
                    ObjectAttribute annotation = field.getAnnotation(ObjectAttribute.class);
                    if(jsonObject.has(annotation.name())) {
                        deserializeJSONObject(jsonObject.getJSONObject(annotation.name()), field.get(target));
                    }
                    else
                    {
                        if(annotation.required()) throw new JSONException("Missing attribute "+annotation.name());
                        field.set(target, null);
                    }
                }
            }
        }
        catch (IllegalAccessException | JSONException error) {
            throw new FileTransferException("JSON Error: "+error.toString()+"->"+error.getMessage());
        }
    }

}
