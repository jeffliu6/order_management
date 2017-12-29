import io.ebean.PagedList;
import models.Entry;
import org.junit.Test;
import play.Application;
import play.inject.guice.GuiceApplicationBuilder;
import play.test.WithApplication;
import repository.EntryRepository;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class ModelTest extends WithApplication {

    @Override
    protected Application provideApplication() {
        return new GuiceApplicationBuilder().build();
    }

    private String formatted(Date date) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    @Test
    public void findById() {
        final EntryRepository entryRepository = app.injector().instanceOf(EntryRepository.class);
        final CompletionStage<Optional<Entry>> stage = entryRepository.lookup(21L);

        await().atMost(1, SECONDS).until(() ->
            assertThat(stage.toCompletableFuture()).isCompletedWithValueMatching(entryOptional -> {
                final Entry macintosh = entryOptional.get();
                return (macintosh.name.equals("Macintosh") && formatted(macintosh.introduced).equals("1984-01-24"));
            })
        );
    }
    
    @Test
    public void pagination() {
        final EntryRepository entryRepository = app.injector().instanceOf(EntryRepository.class);
        CompletionStage<PagedList<Entry>> stage = entryRepository.page(1, 20, "name", "ASC", "");

        // Test the completed result
        await().atMost(1, SECONDS).until(() ->
            assertThat(stage.toCompletableFuture()).isCompletedWithValueMatching(entries ->
                entries.getTotalCount() == 574 &&
                entries.getTotalPageCount() == 29 &&
                entries.getList().size() == 20
            )
        );
    }
    
}
