package org.plantagonist.core.repositories;


import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;


import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;


public class BaseRepository<T> {
    protected final MongoCollection<T> coll;


    public BaseRepository(MongoCollection<T> coll) { this.coll = coll; }


    public void insert(T t) { coll.insertOne(t); }


    public List<T> findAll() {
        List<T> list = new ArrayList<>();
        coll.find().into(list); return list;
    }


    public T findById(String id, Function<T, String> idGetter) {
        for (T t : coll.find()) if (idGetter.apply(t).equals(id)) return t;
        return null;
    }


    public long deleteById(String id, Function<T, String> idGetter) {
        List<Bson> filters = new ArrayList<>();
        filters.add(Filters.eq("id", id));
        return coll.deleteOne(Filters.and(filters)).getDeletedCount();
    }


    public void replace(T t, String id, Function<T, String> idGetter) {
        coll.replaceOne(Filters.eq("id", id), t);
    }
}