package com.sgck.sync;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

import org.mapdb.DataIO;
import org.mapdb.Serializer;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.core.serializer.support.SerializingConverter;

import com.sgck.core.amf.Amf3Input;
import com.sgck.core.amf.Amf3Output;

/**
 * 
 * @author yuan
 * 2015-9-16上午8:48:18
 * @param <A>
 */
public class AMFSerializer<A> extends Serializer<A> implements Serializable{

	 @Override
     public void serialize(DataOutput out, Object value) throws IOException {
         Amf3Output amfWriter = new Amf3Output();
         amfWriter.setOutputStream((OutputStream)out);
         amfWriter.writeObject(value);
         amfWriter.flush();
     }

     @Override
     public A deserialize(DataInput in, int available) throws IOException {
         try {
        	 Amf3Input amfReader = new Amf3Input();
        	 amfReader.setInputStream(new DataIO.DataInputToStream(in));
        	 return (A)amfReader.readObject();
         } catch (ClassNotFoundException e) {
             throw new IOException(e);
         }
     }
}
