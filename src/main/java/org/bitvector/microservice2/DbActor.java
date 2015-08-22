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
        TypedQuery<ProductEntity> query = em.createQuery("SELECT p FROM ProductEntity p", ProductEntity.class);
        List<ProductEntity> productEntities = query.getResultList();
        // return productEntities;
        em.close();
    }

    private void getProductById(GetProductById msg) {
        EntityManager em = emf.createEntityManager();
        ProductEntity productEntity = em.find(ProductEntity.class, msg.getId());
        // return productEntity
        em.close();
    }

    private void addProduct(AddProduct msg) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        em.persist(msg.getProductEntity());
        tx.commit();
        em.close();
    }

    private void updateProduct(UpdateProduct msg) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        ProductEntity productEntity = em.find(ProductEntity.class, msg.getProductEntity().getId());
        productEntity.setName(msg.getProductEntity().getName());
        tx.commit();
        em.close();
    }

    private void deleteProduct(DeleteProduct msg) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        em.remove(msg.getProductEntity());
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
        private List<ProductEntity> productEntities;

        public AllProducts(List<ProductEntity> productEntities) {
            this.productEntities = productEntities;
        }

        public List<ProductEntity> getProductEntities() {
            return productEntities;
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
        private ProductEntity productEntity;

        public AProduct(ProductEntity productEntity) {
            this.productEntity = productEntity;
        }

        public ProductEntity getProductEntity() {
            return productEntity;
        }
    }

    public static class AddProduct implements Serializable {
        private ProductEntity productEntity;

        public AddProduct(ProductEntity productEntity) {
            this.productEntity = productEntity;
        }

        public ProductEntity getProductEntity() {
            return productEntity;
        }
    }

    public static class UpdateProduct implements Serializable {
        private ProductEntity productEntity;

        public UpdateProduct(ProductEntity productEntity) {
            this.productEntity = productEntity;
        }

        public ProductEntity getProductEntity() {
            return productEntity;
        }
    }

    public static class DeleteProduct implements Serializable {
        private ProductEntity productEntity;

        public DeleteProduct(ProductEntity productEntity) {
            this.productEntity = productEntity;
        }

        public ProductEntity getProductEntity() {
            return productEntity;
        }
    }
}
