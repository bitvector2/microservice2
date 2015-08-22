package org.bitvector.microservice2;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

public class DbActor extends AbstractActor {
    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private EntityManagerFactory emf;

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
        emf = Persistence.createEntityManagerFactory("microservice");
    }

    private void stop(Stop msg) {
        emf.close();
    }

    private void getAllProducts(GetAllProducts msg) {
        EntityManager em = emf.createEntityManager();
        TypedQuery<Product> query = em.createQuery("SELECT p FROM Product p", Product.class);
        List<Product> products = query.getResultList();
        // return products;
        em.close();
    }

    private void getProductById(GetProductById msg) {
        EntityManager em = emf.createEntityManager();
        Product product = em.find(Product.class, msg.getId());
        // return product
        em.close();
    }

    private void addProduct(AddProduct msg) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        em.persist(msg.getProduct());
        tx.commit();
        em.close();
    }

    private void updateProduct(UpdateProduct msg) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        Product product = em.find(Product.class, msg.getProduct().getId());
        product.setName(msg.getProduct().getName());
        tx.commit();
        em.close();
    }

    private void deleteProduct(DeleteProduct msg) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        em.remove(msg.getProduct());
        tx.commit();
        em.close();
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
