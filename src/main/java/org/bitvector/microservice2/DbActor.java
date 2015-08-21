package org.bitvector.microservice2;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DbActor extends AbstractActor {
    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private SessionFactory sessionFactory;

    public DbActor() {
        receive(ReceiveBuilder
                        .match(Start.class, this::start)
                        .match(Stop.class, this::stop)
                        .match(GetAllProducts.class, this::getAllProducts)
                        .match(GetProductById.class, this::getProductById)
                        .match(AddProduct.class, this::addProduct)
                        .match(UpdateProduct.class, this::updateProduct)
                        .match(DeleteProduct.class, this::deleteProduct)
                        .matchAny(o -> log.error("DbActor received unknown message " + o.toString()))
                        .build()
        );
    }

    private void start(Start msg) {
        Configuration configuration = new Configuration()
                .addAnnotatedClass(Product.class)    // SUPER FUCKING IMPORTANT PER ENTITY
                .configure();
        ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                .applySettings(configuration.getProperties())
                .build();
        try {
            sessionFactory = configuration.buildSessionFactory(serviceRegistry);
        } catch (ServiceException e) {
            log.error("Failed to create DB connection(s): " + e.getMessage());
            context().stop(this.self());
        }
    }

    private void stop(Stop msg) {
        sessionFactory.close();
    }

    private void getAllProducts(GetAllProducts msg) {
        Session session = sessionFactory.openSession();
        List objs = session.createQuery("FROM Product")
                .setCacheable(true)
                .list();
        session.disconnect();

        List<Product> products = new ArrayList<>();
        for (Object obj : objs) {
            products.add((Product) obj);
        }
        // return products;
    }

    private void getProductById(GetProductById msg) {
        Session session = sessionFactory.openSession();
        List products = session.createQuery("FROM Product WHERE id=:ID")
                .setParameter("ID", msg.getId())
                .setCacheable(true)
                .list();
        session.disconnect();

        /*
        if (products.size() > 0) {
             return (Product) products.get(0);
        } else {
             return null;
        }
        */
    }

    private void addProduct(AddProduct msg) {
        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();
        session.save(msg.getProduct());
        tx.commit();
        session.disconnect();
    }

    private void updateProduct(UpdateProduct msg) {
        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();
        session.update(msg.getProduct());
        tx.commit();
        session.disconnect();
    }

    private void deleteProduct(DeleteProduct msg) {
        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();
        session.delete(msg.getProduct());
        tx.commit();
        session.disconnect();
    }

    public static class Start implements Serializable {
    }

    public static class Stop implements Serializable {
    }

    public static class GetAllProducts implements Serializable {
    }

    public static class AllProducts implements Serializable {
        private List<Product> products;

        public AllProducts(List<Product> products) {
            this.products = products;
        }

        public List<Product> getProducts() {
            return products;
        }
    }

    public static class GetProductById implements Serializable {
        private Integer id;

        public GetProductById(Integer id) {
            this.id = id;
        }

        public Integer getId() {
            return id;
        }
    }

    public static class AProduct implements Serializable {
        private Product product;

        public AProduct(Product product) {
            this.product = product;
        }

        public Product getProduct() {
            return product;
        }
    }

    public static class AddProduct implements Serializable {
        private Product product;

        public AddProduct(Product product) {
            this.product = product;
        }

        public Product getProduct() {
            return product;
        }
    }

    public static class UpdateProduct implements Serializable {
        private Product product;

        public UpdateProduct(Product product) {
            this.product = product;
        }

        public Product getProduct() {
            return product;
        }
    }

    public static class DeleteProduct implements Serializable {
        private Product product;

        public DeleteProduct(Product product) {
            this.product = product;
        }

        public Product getProduct() {
            return product;
        }
    }
}
