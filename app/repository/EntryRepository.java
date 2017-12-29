package repository;

import io.ebean.*;
import models.Entry;
import play.db.ebean.EbeanConfig;

import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * A repository that executes database operations in a different
 * execution context.
 */
public class EntryRepository {

    private final EbeanServer ebeanServer;
    private final DatabaseExecutionContext executionContext;

    @Inject
    public EntryRepository(EbeanConfig ebeanConfig, DatabaseExecutionContext executionContext) {
        this.ebeanServer = Ebean.getServer(ebeanConfig.defaultServer());
        this.executionContext = executionContext;
    }

    /**
     * Return a paged list of entry
     *
     * @param page     Page to display
     * @param pageSize Number of entries per page
     * @param sortBy   Entry property used for sorting
     * @param order    Sort order (either or asc or desc)
     * @param filter   Filter applied on the name column
     */
    public CompletionStage<PagedList<Entry>> page(int page, int pageSize, String sortBy, String order, String filter) {
        return supplyAsync(() ->
                ebeanServer.find(Entry.class).where()
                    .ilike("name", "%" + filter + "%")
                    .orderBy(sortBy + " " + order)
                    .fetch("company")
                    .setFirstRow(page * pageSize)
                    .setMaxRows(pageSize)
                    .findPagedList(), executionContext);
    }

    public CompletionStage<Optional<Entry>> lookup(Long id) {
        return supplyAsync(() -> Optional.ofNullable(ebeanServer.find(Entry.class).setId(id).findOne()), executionContext);
    }

    public CompletionStage<Optional<Long>> update(Long id, Entry newEntryData) {
        return supplyAsync(() -> {
            Transaction txn = ebeanServer.beginTransaction();
            Optional<Long> value = Optional.empty();
            try {
            	Entry savedEntry = ebeanServer.find(Entry.class).setId(id).findOne();
                if (savedEntry != null) {
                    savedEntry.company = newEntryData.company;
                    savedEntry.end_date = newEntryData.end_date;
                    savedEntry.start_date = newEntryData.start_date;
                    savedEntry.name = newEntryData.name;

                    savedEntry.update();
                    txn.commit();
                    value = Optional.of(id);
                }
            } finally {
                txn.end();
            }
            return value;
        }, executionContext);
    }

    public CompletionStage<Optional<Long>>  delete(Long id) {
        return supplyAsync(() -> {
            try {
                final Optional<Entry> entryOptional = Optional.ofNullable(ebeanServer.find(Entry.class).setId(id).findOne());
                entryOptional.ifPresent(Model::delete);
                return entryOptional.map(c -> c.id);
            } catch (Exception e) {
                return Optional.empty();
            }
        }, executionContext);
    }

    public CompletionStage<Long> insert(Entry entry) {
        return supplyAsync(() -> {
             entry.id = System.currentTimeMillis(); // not ideal, but it works
             ebeanServer.insert(entry);
             return entry.id;
        }, executionContext);
    }
}
