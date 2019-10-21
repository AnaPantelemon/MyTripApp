package com.myTrips.controller;

import com.myTrips.model.Trip;
import com.myTrips.model.User;
import com.myTrips.persistance.TripRepository;
import com.myTrips.persistance.UserRepository;
import com.myTrips.service.FileUploadService;
import com.myTrips.service.MyUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;
import javax.validation.Valid;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
/*
This class is responsible for taking all the requests coming from:addtrips,edit trips and delete trips pages.
This class is also responsible for rendering the main page from the application:trips-page.
In the main page there is a drop down input field from which a trip can be selected and all the other fields will
be populated with relevant details from that trip.
Click-ing on ADD button from trips-page the addtrips page wil be displayed.There are several fields that must
be filed in(trip name,startDate,endDate,location etc) but the mandatory one is trip name which must be unique.
Start date and end date will be validated as well (to be in the past or current day and start day must be prior end date)
Click-ing on EDIT button will display edit trips page and all the fields can be updated on request.
Click-ing on DELETE button will display delete trips page.Selected trip can be deleted. A link on the page is provided in case
in case of second thoughts.

 */


@Controller
public class TripController {
	
	@Autowired
	//this means to get the bean called userRepository	which is auto-generated by Spring, we will use it to handle the data
	private UserRepository userRepository;
	@Autowired
	private TripRepository tripRepository;
	
	@Autowired
	private FileUploadService fileService;
	
	@Autowired
	private MyUserDetailsService myUserDetailsService;
	@Autowired
	private FileController fileController;
	
	@ModelAttribute
	LocalDate initLocalDate() {
		return LocalDate.ofEpochDay(0);
	}
	
	@PostConstruct
	public void init() {
		// Setting Spring Boot SetTimeZone
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}
	
	@Transactional
	@GetMapping(value = "/trips-page")//shows main page trips-page
	public String updateTripPage(@RequestParam(name = "tripId", required = false) Long tripId,
								 Model model) {
		myUserDetailsService.getUsername();//getting the username of authenticated user
		System.out.println("My user is:" + myUserDetailsService.getUsername());
		User registered = userRepository.findByUsername(myUserDetailsService.getUsername());//getting the user autheticated with above username
		Trip currentTrip = null;
		
		Set<Trip> tripsById = registered.getTrips();
		if (tripId!=null && tripsById.size() > 0) {
			for (Trip trip : tripsById) {
				if (trip.getTripId()==tripId) {
					currentTrip = trip;
					break;
				}
			}
		}
		if (tripsById.size()==0) {
			currentTrip = new Trip(0L, "model trip");//set an empty trip when no trip exists on current user
		}
		if (currentTrip==null && tripsById.size() > 0) {
			currentTrip = tripsById.iterator().next();
		}
		
		if (currentTrip!=null) {
			model.addAttribute("trip", currentTrip);
			model.addAttribute("currentId", currentTrip.getTripId());
			model.addAttribute("tripsById", tripsById);
			model.addAttribute("impressions", currentTrip.getImpressions());
			model.addAttribute("username", registered.getUsername());
			model.addAttribute("UUID1", currentTrip.getPhoto1());
			model.addAttribute("UUID2", currentTrip.getPhoto2());
			model.addAttribute("id", currentTrip.getTripId());
			currentTrip.setUser(registered);
		}
	
		return "trips-page";
	}
	
	@GetMapping("/addtrips")//shows addtrips page
	public String showTripPage(Model model, Trip trip) {
		String username = myUserDetailsService.getUsername();
		model.addAttribute("username", username);
		model.addAttribute("trip", trip);
		
		return "addtrips";
	}
	
	//This method is adding a new trip data to server based on user input
	@PostMapping("/addtrips")
	public String saveTrip(@RequestParam("files") MultipartFile[] files,
						   @Valid @ModelAttribute("trip") Trip trip, BindingResult bindingResult, Model model) {
		
		
		User registered = userRepository.findByUsername(myUserDetailsService.getUsername());//get authenticated user based on username
		
		if ((trip.getStartDate()!=null) && (trip.getEndDate()!=null)) {//creating validation for startDate to be prior to endDate
			if (trip.getEndDate().isBefore(trip.getStartDate())) {
				bindingResult.rejectValue("endDate", "error.trip", "Start date must be prior end date");
			}
		}
		if (bindingResult.hasErrors()) {
			model.addAttribute("username", registered.getUsername());
			return "addtrips";
		}
		model.addAttribute("tripId", trip.getTripId());
		model.addAttribute("tripname", trip.getTripname());
		model.addAttribute("username", registered.getUsername());
		model.addAttribute("startDate", trip.getStartDate());
		model.addAttribute("endDate", trip.getEndDate());
		model.addAttribute("impressions", trip.getImpressions());
		model.addAttribute("description1", trip.getDescription1());
		model.addAttribute("description2", trip.getDescription2());
		model.addAttribute("title1", trip.getTitle1());
		model.addAttribute("title2", trip.getTitle2());
		
		Set<Trip> tripsById = registered.getTrips();//getting a set of trips for registered user
		model.addAttribute("tripsById", tripsById);
		
		trip.setPhoto1(UUID.randomUUID().toString()); // generate UUID and save it to server on a column called photo1
		trip.setPhoto2(UUID.randomUUID().toString()); // generate UUID and save it to server on a column called photo2
		trip.setUser(registered);//setting the registered user to current trip
		Trip savedTrip = tripRepository.save(trip);//saves trip to tripRepository
//		registered.addTrip(savedTrip);
		
		fileService.store(trip.getPhoto1(), files[0]);//storing the first picture from multipart array
		fileService.store(trip.getPhoto2(), files[1]);//storing second picture from multipart array
		
		model.addAttribute("UUID1", trip.getPhoto1());
		model.addAttribute("UUID2", trip.getPhoto2());
		
		return "redirect:/trips-page";
	}
	
	//This method is editing a trip data to server based on user input
	@RequestMapping(value = "/edit/{tripId}", method = RequestMethod.GET)
	public String showTripFields(@PathVariable("tripId") Long tripId, Model model) {
		
		Trip currentTrip=tripRepository.findById(tripId).get();//get authenticated user based on username
		if(currentTrip!=null) {
			model.addAttribute("id", tripId);
			model.addAttribute("trip", tripRepository.findById(tripId));
			model.addAttribute("username", currentTrip.getUser().getUsername());
			
		}

		return "edit";
	}
	//this method is responsible for saving all changed fields on database USERS on trip table
	@PostMapping("/edit/{tripId}")
	public String updateTrip(@PathVariable("tripId") Long tripId,
							 @Valid @ModelAttribute("trip") Trip trip, BindingResult result,
							 @RequestParam("files") MultipartFile[] files,
							 Model model) {
		
		User registered = userRepository.findByUsername(myUserDetailsService.getUsername());//getting the registered user
		if ((trip.getStartDate()!=null) && (trip.getEndDate()!=null)) {//creating validation for startDate to be prior to endDate
			if (trip.getEndDate().isBefore(trip.getStartDate())) {
				result.rejectValue("endDate", "error.trip", "Start date must be prior end date");
			}
		}
		
		if (result.hasErrors()) {
			trip.setTripId(tripId);
			return "addtrips";
		}
		//condition for first image
			if (files[0].isEmpty()) {//if no new image is selected then same image will be displayed o trips-page
				model.addAttribute("UUID1", trip.getPhoto1());
			} else {
				trip.setPhoto1(UUID.randomUUID().toString());//if a new image is selected than a new UUID is generated and stored in database
				fileService.store(trip.getPhoto1(), files[0]);//store the new image
				model.addAttribute("UUID1", trip.getPhoto1());
			}
		//condition for second image
			if (files[1].isEmpty()) {
				model.addAttribute("UUID2", trip.getPhoto2());
			} else {
				trip.setPhoto2(UUID.randomUUID().toString());
				fileService.store(trip.getPhoto2(), files[1]);
				model.addAttribute("UUID2", trip.getPhoto2());
			}
		
	//getting a set of trips of the registered user
		Set<Trip> tripsById = registered.getTrips();
		
		trip.setTripId(tripId);
		trip.setUser(registered);
		tripRepository.save(trip);//saving in the database edited trip

		
	
		model.addAttribute("username", registered.getUsername());
		model.addAttribute("trip", trip);
		model.addAttribute("tripsById", tripsById);
		model.addAttribute("id", trip.getTripId());
		return "redirect:/trips-page";
	}
//this method is displaying delete trip page
	@GetMapping(value = "/delete/{tripId}")
	public String getTrip(@PathVariable("tripId") Long tripId,Model model) {
		model.addAttribute("trip",tripRepository.findById(tripId).get());
		model.addAttribute("tripId",tripId);
		model.addAttribute("id",tripId);
		return "delete";
	}
	//this method is responsible for deleting the trip of current selection from main page
	@RequestMapping(value = "/delete/{tripId}",method = RequestMethod.POST)
	public String deleteTrip(@PathVariable("tripId") Long id) {
	
		User registered = userRepository.findByUsername(myUserDetailsService.getUsername());
		
		tripRepository.deleteById(id);
		
		return "redirect:/trips-page";
	}

}