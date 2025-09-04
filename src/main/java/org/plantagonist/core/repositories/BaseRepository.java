package org.plantagonist.core.repositories;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class BaseRepository<T> {
    protected final MongoCollection<T> coll;

    public BaseRepository(MongoCollection<T> coll) {
        this.coll = coll;
    }

    /** Canonical id filter (schema uses _id:String UUID).
     *  If you have legacy docs with an "id" field, temporarily switch to the OR version.
     */
    protected Bson idFilter(String id) {
        // return Filters.or(Filters.eq("_id", id), Filters.eq("id", id)); // <-- use during migration
        return Filters.eq("_id", id);
    }

    protected Bson idFilter(ObjectId id) {
        return Filters.eq("_id", id);
    }

    public void insert(T t) { coll.insertOne(t); }

    public List<T> findAll() {
        List<T> list = new ArrayList<>();
        coll.find().into(list);
        return list;
    }

    public T findById(String id, Function<T, String> idGetter) {
        // Ignore idGetter; canonical key is _id.
        return coll.find(idFilter(id)).first();
    }

    public long deleteById(String id, Function<T, String> idGetter) {
        var res = coll.deleteOne(idFilter(id));
        System.out.println("[DB] deleteById _id=" + id + " deleted=" + res.getDeletedCount());
        return res.getDeletedCount();
    }

    protected void replace(T entity, String id, Function<T, String> idGetter) {
        var result = coll.replaceOne(idFilter(id), entity);
        System.out.println("[DB] replace _id=" + id +
                " matched=" + result.getMatchedCount() +
                " modified=" + result.getModifiedCount());
    }

    public List<T> findByUserId(String userId, Function<T, String> userIdGetter) {
        List<T> out = new ArrayList<>();
        coll.find(Filters.eq("userId", userId)).into(out);
        return out;
    }

    public List<T> findByField(String fieldName, String value) {
        List<T> out = new ArrayList<>();
        coll.find(Filters.eq(fieldName, value)).into(out);
        return out;
    }

}
