/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mx.edu.um.mateo.rh.service.impl;

/**
 *
 * @author IrasemaBalderas
 */
import java.util.Map;
import mx.edu.um.mateo.general.utils.Constantes;
import mx.edu.um.mateo.general.utils.ObjectRetrievalFailureException;
import mx.edu.um.mateo.rh.dao.ColegioDao;
import mx.edu.um.mateo.rh.model.Colegio;
import mx.edu.um.mateo.rh.service.ColegioManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ColegioManagerImpl implements ColegioManager {

    @Autowired
    private ColegioDao dao;

    /**
     * @see
     * mx.edu.um.rh.service.ColegioManager#getColegios(mx.edu.um.rh.model.Colegio)
     */
    @Override
    public Map<String, Object> lista(Map<String, Object> params) {
        return dao.lista(params);
    }

    /**
     * @see mx.edu.um.rh.service.ColegioManager#obtiene(String id)
     */
    @Override
    public Colegio obtiene(final Long id) throws ObjectRetrievalFailureException{
        return dao.obtiene(id);
    }

    /**
     * @see mx.edu.um.rh.service.ColegioManager#crea(Colegio colegio)
     */
    @Override
    public void crea(Colegio colegio) {
        if(colegio.getId() == null){
            colegio.setStatus(Constantes.STATUS_ACTIVO);
        }
        dao.crea(colegio);
    }
    
   

    /**
     * @see mx.edu.um.rh.service.ColegioManager#elimina(String id)
     */
    @Override
    public String elimina(final Long id) {
         return dao.elimina(id);
    }
}
