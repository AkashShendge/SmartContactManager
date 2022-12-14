package com.acks.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;
import javax.transaction.Transactional;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.acks.dao.ContactRepository;
import com.acks.dao.MyOrderRepository;
import com.acks.dao.UserRepository;
import com.acks.helper.Message;
import com.acks.model.Contact;
import com.acks.model.MyOrder;
import com.acks.model.Users;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

@Controller
@RequestMapping("/user")
public class UserController {

	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ContactRepository contactRepository;

	@Autowired
	private MyOrderRepository myOrderRepository;

	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {
		String userName = principal.getName();
		System.out.println("UserName = " + userName);

		Users users = this.userRepository.getUsersByUserName(userName);
		System.out.println("User = " + users);

		model.addAttribute("users", users);
	}

	@RequestMapping("/index")
	public String dashboardHandler(Model model, Principal principal) {
		return "normal/user_dashboard";
	}

	// Add Contact Handler
	@GetMapping("add-contact")
	public String openAddContactHandler(Model model) {
		model.addAttribute("title", "Add Contact");
		model.addAttribute("contact", new Contact());

		return "normal/add_contact_form";
	}

	// Processing Add Contact Handler
	@PostMapping("/process-contact")
	public String processConatctHandler(@ModelAttribute("contact") Contact contact,
			@RequestParam("profileImages") MultipartFile file, Principal principal, HttpSession session) {
		try {
			String name = principal.getName();
			Users users = this.userRepository.getUsersByUserName(name);

			contact.setUsers(users);
			System.out.println("------" + users);
			if (file.isEmpty()) {
				// if file is empty then try our message
				contact.setImage("contact.png");
			} else {
				// file the file to folder and update the name to contact
				contact.setImage(file.getOriginalFilename());

				File savefile = new ClassPathResource("static/image").getFile();

				Path path = Paths.get(savefile.getAbsolutePath() + File.separator + file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			}

			users.getContacts().add(contact);
			this.userRepository.save(users);

			System.out.println("Contact = " + contact);
			System.out.println("Add to data base");

			session.setAttribute("message", new Message("Your contact is added !!", "success"));

			System.out.println(session.getAttribute("message"));
		} catch (Exception e) {
			// TODO: handle exception
			System.out.println(e.getMessage());
			e.printStackTrace();

			session.setAttribute("message", new Message("Something went to wrong!! Try again...", "danger"));
		}

		return "normal/add_contact_form";
	}

	// Show Contact Handler
	// per page = 5[n]
	// current page = 0 [page]
	@GetMapping("/show-contacts/{page}")
	public String showContactHandler(@PathVariable("page") Integer page, Model model, Principal principal) {
		String userName = principal.getName();
		Users users = this.userRepository.getUsersByUserName(userName);

		// currentPage - page
		// Contact Per page - 5
		Pageable pageable = PageRequest.of(page, 3);

		Page<Contact> contact = this.contactRepository.findContactByUser(users.getId(), pageable);

		model.addAttribute("contact", contact);
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", contact.getTotalPages());

		return "normal/show_contacts";
	}

	// Showing particular contact details
	@GetMapping("/{cId}/contact")
	public String showContactDetailHandler(@PathVariable("cId") Integer cId, Model model, Principal principal) {
		List<Contact> findAll = this.contactRepository.findAll();
		Iterator<Contact> iterator = findAll.iterator();

		Contact contact = null;
		while (iterator.hasNext()) {
			Contact next = iterator.next();
			if (next.getcId() == cId)
				contact = next;
		}

		String userName = principal.getName();
		Users users = this.userRepository.getUsersByUserName(userName);

		if (users.getId() == contact.getUsers().getId()) {
			model.addAttribute("contact", contact);
			model.addAttribute("title", contact.getName());
		}

		return "normal/contact_detail";
	}

	// Delete Contact Handler

	@GetMapping("/delete/{cId}")
	@Transactional
	public String deleteContactHandler(@PathVariable("cId") Integer cId, Principal principal, HttpSession session) {
		Contact contact = this.contactRepository.findById(cId).get();

		String userName = principal.getName();
		Users users = this.userRepository.getUsersByUserName(userName);

//		if(users.getId()==contact.getUsers().getId())
//		{
//			contact.setUsers(null);
//			this.contactRepository.delete(contact);
		users.getContacts().remove(contact);

		this.userRepository.save(users);

		session.setAttribute("message", new Message("Contact deleted Successfully...", "success"));
//		}

		return "redirect:/user/show-contacts/0";
		// return "deletehandler";
	}

	// Open update form handler
	@PostMapping("/update-contact/{cId}")
	public String updateform(@PathVariable("cId") Integer cId, Model model) {
		model.addAttribute("title", "Update contact");

		Contact contact = this.contactRepository.findById(cId).get();
		model.addAttribute("contact", contact);

		return "normal/update_form";
	}

	// Process Update Contact handler
	@PostMapping("/process-update")
	public String updateProcessHandler(@ModelAttribute Contact contact,
			@RequestParam("profileImages") MultipartFile file, Model model, HttpSession session, Principal principal) {
		try {
			// Old contact details
			Contact oldContactDetail = this.contactRepository.findById(contact.getcId()).get();

			if (!file.isEmpty()) {
				// file work...
				// rewrite

				// Delete Old Photo
				File deletefile = new ClassPathResource("static/image").getFile();
				File file1 = new File(deletefile, oldContactDetail.getImage());
				file1.delete();

				// Update Old Photo
				File savefile = new ClassPathResource("static/image").getFile();

				Path path = Paths.get(savefile.getAbsolutePath() + File.separator + file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

				contact.setImage(file.getOriginalFilename());
			} else {
				contact.setImage(oldContactDetail.getImage());
			}

			Users users = this.userRepository.getUsersByUserName(principal.getName());
			contact.setUsers(users);
			this.contactRepository.save(contact);

			session.setAttribute("message", new Message("Your contact is updated...", "success"));

		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("CONTACT NAME = " + contact.getName());
		System.out.println("CONTACT ID = " + contact.getcId());

		return "redirect:/user/" + contact.getcId() + "/contact";
	}

	// Profile Handler
	@GetMapping("/profile")
	public String profileHandler(Model model) {
		model.addAttribute("title", "Profile");
		return "normal/profile";
	}

	// Open Setting Handler
	@GetMapping("/settings")
	public String settingHandler() {
		return "normal/setting";
	}

	// Change Password..handler
	@PostMapping("/change-password")
	public String changePasswordHandler(@RequestParam("oldPassword") String oldPassword,
			@RequestParam("newPassword") String newPassword, Principal principal, HttpSession session) {
		System.out.println("OLD PASSWORD : " + oldPassword);
		System.out.println("NEW PASSWORD : " + newPassword);

		String userName = principal.getName();
		Users currentUser = this.userRepository.getUsersByUserName(userName);
		System.out.println(currentUser.getPassword());

		if (this.bCryptPasswordEncoder.matches(oldPassword, currentUser.getPassword())) {
			// Change the Password
			currentUser.setPassword(this.bCryptPasswordEncoder.encode(newPassword));
			this.userRepository.save(currentUser);

			session.setAttribute("message", new Message("Your Password is changed successfully...", "success"));
		} else {
			// error
			session.setAttribute("message", new Message("Please enter your correct old password !!", "danger"));
			return "redirect:/user/settings";
		}

		return "redirect:/user/index";
	}

	// creating order for payment
	@PostMapping("/create_order")
	@ResponseBody
	public String createOrder(@RequestBody Map<String, Object> data, Principal principal) throws RazorpayException {
		System.out.println(data);

		int amt = Integer.parseInt(data.get("amount").toString());

		RazorpayClient client = new RazorpayClient("rzp_test_Iw3y0w0aj23wxf", "MB77O4XNuccsalnLCq5g0A4t");

		JSONObject ob = new JSONObject();
		ob.put("amount", amt * 100);
		ob.put("currency", "INR");
		ob.put("receipt", "txn_235425");

		// creating new order

		Order order = client.Orders.create(ob);
		System.out.println(order);

		// save the order in database
		MyOrder myOrder = new MyOrder();
		myOrder.setAmount(order.get("amount") + "");
		myOrder.setOrderId(order.get("id"));
		myOrder.setPaymentId(null);
		myOrder.setStatus("created");
		myOrder.setUsers(this.userRepository.getUsersByUserName(principal.getName()));
		myOrder.setReceipt(order.get("receipt"));

		this.myOrderRepository.save(myOrder);

		// if you want you can save this to your data...
		return order.toString();
	}

	@PostMapping("/update_order")
	public ResponseEntity<?> updateOrder(@RequestBody Map<String, Object> data) {
		MyOrder myOrder = this.myOrderRepository.findByOrderId(data.get("order_id").toString());
		myOrder.setPaymentId(data.get("payment_id").toString());
		myOrder.setStatus(data.get("status").toString());

		this.myOrderRepository.save(myOrder);

		System.out.println(data);
		Map<Object,Object> map = new HashMap<>();
		map.put("msg", "updated");

		return ResponseEntity.ok(map);
	}

}
