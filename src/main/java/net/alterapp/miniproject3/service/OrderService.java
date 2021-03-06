package net.alterapp.miniproject3.service;

import net.alterapp.miniproject3.domain.Book;
import net.alterapp.miniproject3.domain.Customer;
import net.alterapp.miniproject3.domain.Library;
import net.alterapp.miniproject3.domain.Order;
import net.alterapp.miniproject3.exception.ErrorCode;
import net.alterapp.miniproject3.exception.ServiceException;
import net.alterapp.miniproject3.repository.OrderRepo;
import org.joda.time.DateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private final OrderRepo orderRepo;
    private final LibraryService libraryService;
    private final BookService bookService;
    private final CustomerService customerService;

    public OrderService(OrderRepo orderRepo, LibraryService libraryService, BookService bookService, CustomerService customerService) {
        this.orderRepo = orderRepo;
        this.libraryService = libraryService;
        this.bookService = bookService;
        this.customerService = customerService;
    }

    public List<Order> findAll() {
        return orderRepo.findAllByDeletedAtIsNull();
    }

    public Order findById(Long id) {
        return orderRepo.findByIdAndDeletedAtIsNull(id);
    }

    public List<Order> findAllByOrder(Long id) {
        List<Order> orders = new ArrayList<>();
        Book book = bookService.findById(id);
        if (book != null) {
            orders = orderRepo.findAllByBookId(id);
        } else {
            System.out.println("There is no Book with id = " + id);
        }
        return orders;
    }

    public Order add(Order order) throws ServiceException {
        Library library = libraryService.findById(order.getLibrary().getId());
        List<Book> books = bookService.findAllByLibrary(library.getId());
        Customer customer = customerService.findById(order.getCustomer().getId());
        DateTime dateTime = new DateTime();
        order.setPassingDate(dateTime.plusMonths(1).toDate());
        order.setLibrary(library);
        order.setCustomer(customer);
        boolean isEquals = false;
        for (Book book : books) {
            if (order.getBook().getId().equals(book.getId())) {
                isEquals = true;
                order.setBook(book);
                book.setAvailableBook(0);
            }
        }

        List<Order> orders = orderRepo.findAllByBookId(order.getBook().getId());
        boolean isNotContainsBook = true;
        for(Order order1 : orders ) {
            if(order1.getPassedDate() == null) {
                isNotContainsBook = false;
            }
        }

        if (isEquals) {
            if(isNotContainsBook){
                return orderRepo.save(order);
            }else {
                throw ServiceException.builder()
                        .errorCode(ErrorCode.SYSTEM_ERROR)
                        .httpStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .message("Book is not available")
                        .build();
            }
        } else {
            throw ServiceException.builder()
                    .errorCode(ErrorCode.SYSTEM_ERROR)
                    .httpStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                    .message("there is no book like this in given library")
                    .build();
        }
    }

    public String update(Long id) {
        Order order= orderRepo.findByIdAndDeletedAtIsNull(id);
        order.setPassingDate(new Date());
        orderRepo.save(order);
        return "update";
    }

    public Order updatePassedDate(Long id) {
        Order order= orderRepo.findByIdAndDeletedAtIsNull(id);

        Library library = libraryService.findById(order.getLibrary().getId());
        List<Book> books = bookService.findAllByLibrary(library.getId());
        Customer customer = customerService.findById(order.getCustomer().getId());

        if(order.getPassedDate() == null) {
            order.setPassedDate(new Date());
            orderRepo.save(order);
            for (Book book : books) {
                    bookService.upd(book.getId());
                }
        }
        return order;
    }

}


