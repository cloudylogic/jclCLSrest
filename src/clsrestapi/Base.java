/*
 * Copyright 2017 Ken Lowrie.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package clsrestapi;

import java.net.*;
import java.io.*;
import java.util.Collections;

/**
 * This is the base class for the CLS REST API set. Each API contains three
 * objects, a DbgObj object, an ApiVer object and an API-specific object which
 * will be defined by the class that extends the base class.
 * 
 * This is marked as an abstract class because we have one method, parseJSON,
 * which must be implemented by the consumer.
 * 
 * @author Ken Lowrie
 * @param <T> When this class is extended, you must specify the class of the
 * extender that will be returned by the abstract methods within.
 */
public abstract class Base<T> implements Serializable{

    /**
     * This URL is the fully qualified URL for this API instance.
     */
    protected String webServiceUrl;
    
    /**
     * This is the String name of the current API instance. 
     */
    protected String apiName;

    /**
     * This holds the dbgObj JSON object from a CLS REST API call.
     */
    public DbgObj dbgObj;
    
    /**
     * This holds the apiVer JSON object from a CLS REST API call.
     */
    public ApiVer apiVer;
    
    /**
     * This is the constructor for the Base class. You pass in the host path
     * and the apiName, and the fully qualified name is constructed on the fly.
     * This allows you to override the host path, primarily useful for debugging.
     * 
     * @param wsUrlRoot The host name to issue the API call to. Default is
     * http://api.cloudylogic.com.
     * 
     * @param apiName The apiName for this object instance. e.g. reels.
     */
    public Base(String wsUrlRoot,String apiName){
        this.apiName = apiName;
        this.webServiceUrl = makeUrl(wsUrlRoot);
    }
    
    /**
     * Constructs the web service URL for this instance. The apiName has
     * been filled out in the constructor before this method is invoked.
     * 
     * TODO: not liking the apiname override ...
     * 
     * @param wsUrlRoot The web service URL. If no ending '/', it will be added.
     * @return The fully qualified URL for the API.
     */
    private String makeUrl(String wsUrlRoot){        
        StringBuilder s = new StringBuilder(wsUrlRoot);
        
        if (!wsUrlRoot.endsWith("/")) s.append('/');
        
        s.append(apiName);
        
        if (!apiName.endsWith("/")) s.append('/');
        
        return s.toString();
    }
    
    /**
     * This method loads the JSON data from a web service. If it does not see
     * a header of Content-Type == application/json, it will throw an 
     * AssertionError.
     * 
     * @return String containing the JSON data from the web service.
     */
    public String loadJSONfromWebService() throws CRAException {
        try{
            URL url = new URL(webServiceUrl);
            
            URLConnection conn = url.openConnection();
            
            if(!"application/json".equals(conn.getHeaderField("Content-Type"))){
                throw new CRAException("Wrong Content-Type");
            }
        
            InputStream is = url.openStream();

            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            String json = "";
            String current;

            while((current = in.readLine()) != null) {
               json += current;
            }
            return json;
        }catch(IOException e){
            // Catch the exception thrown by url.openStream() and translate it
        }
        throw new CRAException("URL [" + webServiceUrl + "] not available.");
    }
    /**
     * This method returns a string heading used by the derived class in
     * the toString() override. Basically just a dashed line, the name of
     * the API, and another dashed line, making it easy to delineate the
     * various API calls being made. TODO: Might want to make this optional,
     * in case users of toString() want to do their own formatting...
     * 
     * @return String heading separator.
     */
    public String getHeader(){
        String dashes = String.join("", Collections.nCopies(40, "-"));

        StringBuilder s = new StringBuilder(dashes);
        
        s.append(Constants.NL);
        s.append("API:");
        s.append(apiName);
        s.append(Constants.NL);
        s.append(dashes);
        s.append(Constants.NL);

        return s.toString();
    }
    
    /**
     * This method serializes an object instance to the specified disk file.
     * @param filename is the name of the file to write to. It is overwritten
     * if it already exists.
     * @return boolean indicating whether the serialization was successful.
     */
    public boolean serialize(String filename) throws CRAException {
        boolean retVal = true;
        
        // save the object to file
        FileOutputStream fos;
        ObjectOutputStream out;
        try {
            fos = new FileOutputStream(filename);
            out = new ObjectOutputStream(fos);
            out.writeObject(this);

            out.close();
        } catch (FileNotFoundException ex){
            throw new CRAException("File or Directory does not exist.");
        } catch (IOException ex) {
            throw new CRAException("IO error writing to serialization file.");
        }
        return retVal;
    }
    
    /**
     * This method de-serializes an object instance from the specified disk file.
     * The type of object returned is the type of the instance that it is invoked
     * from.
     * @param filename is the name of the file to read from.
     * @return T instance
     */
    public T deSerialize(String filename) throws CRAException {
        T obj;
        // save the object to file
        FileInputStream fis;
        ObjectInputStream in;
        try {
            fis = new FileInputStream(filename);
            in = new ObjectInputStream(fis);
            obj = (T) in.readObject();
            in.close();
        } catch (InvalidClassException | FileNotFoundException ex){
            throw new CRAException(ex.getMessage());
        } catch (IOException | ClassNotFoundException ex) {
            throw new CRAException(ex.getMessage());
        }
        return obj;
    }
    
    /**
     * This method uses the serialize() and deSerialize() methods of the class
     * to write and then read back the current objects instance data to/from
     * disk. The contents of the newly loaded instance is compared to the 
     * object that was originally written. Messages are written to the console
     * to indicate the progress and status of the operation. This method is
     * meant to be used for testing the underlying code and may go away once
     * I write the JUnit code for this class.
     * @param filename the name of the file to write to. If it exists, it is
     * overwritten.
     */
    public void dumpAndLoad(String filename){
        System.out.println("Serializing class type " + this.getClass());
        try {
            if ( this.serialize(filename) ){
                System.out.println("serialization complete...");
            }
        } catch (CRAException e) {
            System.out.println("write to disk failed: " + e.getMessage());            
        }
        System.out.println("deSerializing class type " + this.getClass());
        try {
            T obj2 = this.deSerialize(filename);
            if ( this.equals(obj2) ){
                System.out.println("deSerialized object type " + this.getClass() + " matches original ...");
            } else {
                System.out.println("deSerialized object does NOT match original ...");                
            }
        } catch (CRAException e){
            System.out.println("read from disk failed: " + e.getMessage());
        }
    }
    
    /**
     * This is an abstract method for the base class. Consumers of this class
     * are required to override this method with the code to load the object
     * by initiating the network call and parsing the JSON.
     * 
     * @return The type of the class that extends this class.
     */
    public abstract T load();
    
    /**
     * Override the built-in toString and properly dump this instance.
     * @return String containing the dbgObj and apiVer instance data for this object.
     */
    @Override
    public String toString(){
        String s;
        
        s = "dbgObj:" + Constants.NL + dbgObj.toString() + Constants.NL + 
            "apiVer:" + Constants.NL + apiVer.toString() + Constants.NL;
        
        return s;
    }
    
}
