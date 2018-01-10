package controllers;

import models.Entry;
import models.Vendor;
import play.data.Form;
import play.data.FormFactory;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Results;
import repository.VendorRepository;
import repository.EntryRepository;
import repository.DeptRepository;

import javax.inject.Inject;
import javax.persistence.PersistenceException;

import io.ebean.Ebean;

import static java.util.concurrent.CompletableFuture.supplyAsync;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CompletableFuture;

/**
 * Manage a database of entries
 */
public class HomeController extends Controller {

	private final EntryRepository entryRepository;
	private final VendorRepository vendorRepository;
	private final DeptRepository deptRepository;
	private final FormFactory formFactory;
	private final HttpExecutionContext httpExecutionContext;

	@Inject
	public HomeController(FormFactory formFactory,
			EntryRepository entryRepository,
			VendorRepository vendorRepository,
			DeptRepository deptRepository,
			HttpExecutionContext httpExecutionContext) {
		this.entryRepository = entryRepository;
		this.formFactory = formFactory;
		this.vendorRepository = vendorRepository;
		this.deptRepository = deptRepository;
		this.httpExecutionContext = httpExecutionContext;
	}

	/**
	 * This result directly redirect to application home.
	 */
	private Result GO_HOME = Results.redirect(
			routes.HomeController.list(0, "name", "asc", "")
			);

	/**
	 * Handle default path requests, redirect to entries list
	 */
	public Result index() {
		return GO_HOME;
	}

	/**
	 * Display the paginated list of entries.
	 *
	 * @param page   Current page number (starts from 0)
	 * @param sortBy Column to be sorted
	 * @param order  Sort order (either asc or desc)
	 * @param filter Filter applied on entry names
	 */
	public CompletionStage<Result> list(int page, String sortBy, String order, String filter) {
		// Run a db operation in another thread (using DatabaseExecutionContext)
		return entryRepository.page(page, 10, sortBy, order, filter).thenApplyAsync(list -> {
			// This is the HTTP rendering thread context
			return ok(views.html.list.render(list, sortBy, order, filter));
		}, httpExecutionContext.current());
	}

	private static class Pair<A, B> {
		A a;
		B b;

		public Pair(A a, B b) {
			this.a = a;
			this.b = b;
		}
	}

	/**
	 * Display the 'edit form' of an existing Entry.
	 *
	 * @param id Id of the  edit
	 */
	public CompletionStage<Result> edit(Long id) {

		// Run a db operation in another thread (using DatabaseExecutionContext)
		CompletionStage<Map<String, String>> vendorsFuture = vendorRepository.options();
		CompletionStage<Map<String, String>> deptsFuture = deptRepository.options();
		Pair<CompletionStage<Map<String, String>>,CompletionStage<Map<String, String>>> tempPair = new Pair<CompletionStage<Map<String, String>>,CompletionStage<Map<String, String>>>(vendorsFuture,deptsFuture);
		CompletableFuture<Pair<CompletionStage<Map<String, String>>,CompletionStage<Map<String, String>>>> myFuture = CompletableFuture.completedFuture(tempPair);

		return entryRepository.lookup(id).thenCombineAsync(myFuture,
				(entryOptional, myPair) -> {
					// This is the HTTP rendering thread context
					Map<String,String> x;
					Map<String,String> y;
					try {
						x = myPair.a.toCompletableFuture().get();
						y = myPair.b.toCompletableFuture().get();
						//System.out.println("Success");
					}
					catch (Exception e){
						x = new HashMap<String,String>();
						y = new HashMap<String,String>();
					}

					Entry c = entryOptional.get();
					Form<Entry> entryForm = formFactory.form(Entry.class).fill(c);
					return ok(views.html.editForm.render(id, entryForm, x,y));//myPair.a.toCompletableFuture().getNow(new HashMap<String,String>()), myPair.b.toCompletableFuture().getNow(new HashMap<String,String>())));
				}, httpExecutionContext.current());
	}

	/**
	 * Handle the 'edit form' submission
	 *
	 * @param id Id of the entry to edit
	 */
	public CompletionStage<Result> update(Long id) throws PersistenceException {
		CompletionStage<Map<String, String>> vendorsFuture = vendorRepository.options();
		CompletionStage<Map<String, String>> deptsFuture = deptRepository.options();
		Pair<CompletionStage<Map<String, String>>,CompletionStage<Map<String, String>>> tempPair = new Pair<CompletionStage<Map<String, String>>,CompletionStage<Map<String, String>>>(vendorsFuture,deptsFuture);
		CompletableFuture<Pair<CompletionStage<Map<String, String>>,CompletionStage<Map<String, String>>>> myFuture = CompletableFuture.completedFuture(tempPair);

		Form<Entry> entryForm = formFactory.form(Entry.class).bindFromRequest();
		if (entryForm.hasErrors()) {
			// Run vendors db operation and then render the failure case
			return myFuture.thenApplyAsync(myPair -> {
				// This is the HTTP rendering thread context
				Map<String,String> x;
				Map<String,String> y;
				try {
					x = myPair.a.toCompletableFuture().get();
					y = myPair.b.toCompletableFuture().get();
					//System.out.println("Success");
				}
				catch (Exception e){
					x = new HashMap<String,String>();
					y = new HashMap<String,String>();
				}

				return badRequest(views.html.editForm.render(id, entryForm, x, y));//myPair.a.toCompletableFuture().getNow(new HashMap<String,String>()), myPair.b.toCompletableFuture().getNow(new HashMap<String,String>())));
			}, httpExecutionContext.current());
		} else {
			Entry newEntryData = entryForm.get();
			// Run update operation and then flash and then redirect
			return entryRepository.update(id, newEntryData).thenApplyAsync(data -> {
				// This is the HTTP rendering thread context
				flash("success", "Entry " + newEntryData.name + " has been updated");
				return GO_HOME;
			}, httpExecutionContext.current());
		}
	}

	/**
	 * Display the 'new entry form'.
	 */
	public CompletionStage<Result> create() {
		CompletionStage<Map<String, String>> vendorsFuture = vendorRepository.options();
		CompletionStage<Map<String, String>> deptsFuture = deptRepository.options();
		Pair<CompletionStage<Map<String, String>>,CompletionStage<Map<String, String>>> tempPair = new Pair<CompletionStage<Map<String, String>>,CompletionStage<Map<String, String>>>(vendorsFuture,deptsFuture);
		CompletableFuture<Pair<CompletionStage<Map<String, String>>,CompletionStage<Map<String, String>>>> myFuture = CompletableFuture.completedFuture(tempPair);

		Form<Entry> entryForm = formFactory.form(Entry.class);
		// Run vendors db operation and then render the form
		return myFuture.thenApplyAsync((myPair) -> {
			// This is the HTTP rendering thread context
			Map<String,String> x;
			Map<String,String> y;
			try {
				x = myPair.a.toCompletableFuture().get();
				y = myPair.b.toCompletableFuture().get();
				//System.out.println("Success");
			}
			catch (Exception e){
				x = new HashMap<String,String>();
				y = new HashMap<String,String>();
			}
			return ok(views.html.createForm.render(entryForm, x, y));//myPair.a.toCompletableFuture().getNow(new HashMap<String,String>()), myPair.b.toCompletableFuture().getNow(new HashMap<String,String>())));
		}, httpExecutionContext.current());
	}

	/**
	 * Handle the 'new entry form' submission
	 */
	public CompletionStage<Result> save() {
		CompletionStage<Map<String, String>> vendorsFuture = vendorRepository.options();
		CompletionStage<Map<String, String>> deptsFuture = deptRepository.options();
		Pair<CompletionStage<Map<String, String>>,CompletionStage<Map<String, String>>> tempPair = new Pair<CompletionStage<Map<String, String>>,CompletionStage<Map<String, String>>>(vendorsFuture,deptsFuture);
		CompletableFuture<Pair<CompletionStage<Map<String, String>>,CompletionStage<Map<String, String>>>> myFuture = CompletableFuture.completedFuture(tempPair);

		Form<Entry> entryForm = formFactory.form(Entry.class).bindFromRequest();
		if (entryForm.hasErrors()) {
			// Run vendors db operation and then render the form
			return myFuture.thenApplyAsync(myPair -> {
				// This is the HTTP rendering thread context
				Map<String,String> x;
				Map<String,String> y;
				try {
					x = myPair.a.toCompletableFuture().get();
					y = myPair.b.toCompletableFuture().get();
					//System.out.println("Success");
				}
				catch (Exception e){
					x = new HashMap<String,String>();
					y = new HashMap<String,String>();
				}

				return badRequest(views.html.createForm.render(entryForm, x,y));//myPair.a.toCompletableFuture().getNow(new HashMap<String,String>()), myPair.a.toCompletableFuture().getNow(new HashMap<String,String>())));
			}, httpExecutionContext.current());
		}

		Entry entry = entryForm.get();
		// Run insert db operation, then redirect
		return entryRepository.insert(entry).thenApplyAsync(data -> {
			// This is the HTTP rendering thread context
			flash("success", "Entry " + entry.name + " has been created");
			return GO_HOME;
		}, httpExecutionContext.current());
	}

	/**
	 * Handle entry deletion
	 */
	public CompletionStage<Result> delete(Long id) {
		// Run delete db operation, then redirect
		return entryRepository.delete(id).thenApplyAsync(v -> {
			// This is the HTTP rendering thread context
			flash("success", "Entry has been deleted");
			return GO_HOME;
		}, httpExecutionContext.current());
	}

}
