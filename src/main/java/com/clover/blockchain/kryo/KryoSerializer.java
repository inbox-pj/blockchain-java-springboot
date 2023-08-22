package com.clover.blockchain.kryo;

import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.io.Input;
import com.esotericsoftware.kryo.kryo5.io.Output;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class KryoSerializer {

    private static Kryo kryo;

    @Autowired
    private KryoSerializer(Kryo kryo){
        this.kryo = kryo;
    }

    public static Object deserialize(byte[] bytes) {
        Input input = new Input(bytes);
        Object obj = kryo.readClassAndObject(input);
        input.close();
        return obj;
    }

    public static byte[] serialize(Object object) {
        Output output = new Output(4096, -1);
        kryo.writeClassAndObject(output, object);
        byte[] bytes = output.toBytes();
        output.close();
        return bytes;
    }

}
