package com.practicas.janhout.practicadb4o;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.db4o.Db4oEmbedded;
import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;
import com.db4o.query.Predicate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class Principal extends Activity {

    private ArrayList<Juego> datosJuegos;
    private ListView lista;
    private AdaptadorLista adaptador;
    private SharedPreferences pc;
    private SharedPreferences.Editor ed;

    private ArrayList<String> valoresDificultad;
    private ArrayList<String> valoresPublicacion;
    private ArrayList<String> valoresPuntuacion;
    private ArrayList<String> valoresTipo;

    private int dialogo = -1;
    private int posicion = -1;

    private AlertDialog alerta;

    private final int NUEVO_JUEGO = 1;
    private final int EDITAR_JUEGO = 2;

    private ObjectContainer bd;

    /**********************************************************************************************/
    /**********************************************************************************************/
    /*                                    Métodos on...                                           */
    /**********************************************************************************************/
    /**
     * ******************************************************************************************
     */

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            Juego j = data.getParcelableExtra(getString(R.string.juegoI));
            switch (requestCode) {
                case NUEVO_JUEGO:
                    if (!datosJuegos.contains(j)) {
                        insertarBD(j);
                        datosJuegos.add(j);
                        actualizarLista();
                    } else {
                        tostada(getString(R.string.juegoRepetido));
                        posicion = -1;
                        nuevoJuego(true, j);
                    }
                    break;
                case EDITAR_JUEGO:
                    if (!datosJuegos.contains(j) ||
                            (posicion == datosJuegos.indexOf(j) && datosJuegos.contains(j))) {
                        if (posicion == -1) {
                            insertarBD(j);
                            datosJuegos.add(j);
                        } else {
                            editarBD(datosJuegos.get(posicion), j);
                            datosJuegos.set(posicion, j);
                        }
                        actualizarLista();
                    } else {
                        tostada(getString(R.string.juegoRepetido));
                        nuevoJuego(true, j);
                    }
                    break;
            }
        }
    }

    /*Método que gestiona el clic sobre un elemento del menu contextual*/
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int id = item.getItemId();
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        posicion = info.position;
        if (id == R.id.contextual_borrar) {
            return borrarJuego();
        } else if (id == R.id.contextual_editar) {
            return nuevoJuego(true, datosJuegos.get(posicion));
        }
        return super.onContextItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_principal);
        initComponents();
    }

    /*Método que crea el menú contextual*/
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.contextual, menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.principal, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_nuevo) {
            return nuevoJuego(false, null);
        } else if (id == R.id.menu_ordena_nombre) {
            return cambiaOrdena(1);
        } else if (id == R.id.menu_ordena_tipo) {
            return cambiaOrdena(3);
        } else if (id == R.id.menu_ordena_puntuacion) {
            return cambiaOrdena(2);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (bd != null) {
            bd.close();
            bd = null;
        }
    }

    /*Método para gestionar dinamicamente el menú principal*/
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        switch (pc.getInt(getString(R.string.ordenarI), 1)) {
            case 1:
                menu.findItem(R.id.menu_ordena_puntuacion).setVisible(true);
                menu.findItem(R.id.menu_ordena_nombre).setVisible(false);
                menu.findItem(R.id.menu_ordena_tipo).setVisible(true);
                break;
            case 2:
                menu.findItem(R.id.menu_ordena_puntuacion).setVisible(false);
                menu.findItem(R.id.menu_ordena_nombre).setVisible(true);
                menu.findItem(R.id.menu_ordena_tipo).setVisible(true);
                break;
            case 3:
                menu.findItem(R.id.menu_ordena_puntuacion).setVisible(true);
                menu.findItem(R.id.menu_ordena_nombre).setVisible(true);
                menu.findItem(R.id.menu_ordena_tipo).setVisible(false);
                break;
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        posicion = savedInstanceState.getInt(getString(R.string.posicionI));
        dialogo = savedInstanceState.getInt(getString(R.string.dialogoI));
        switch (dialogo) {
            case 0:
                mostrarFoto();
                break;
            case 1:
                mostrarDatos(datosJuegos.get(posicion));
                break;
            case 2:
                borrarJuego();
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bd == null) {
            bd = Db4oEmbedded.openFile(Db4oEmbedded.newConfiguration(),
                    getExternalFilesDir(null) + "/bd.db4o");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle savingInstanceState) {
        super.onSaveInstanceState(savingInstanceState);
        savingInstanceState.putInt(getString(R.string.dialogoI), dialogo);
        savingInstanceState.putInt(getString(R.string.posicionI), posicion);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (alerta != null)
            alerta.dismiss();
    }

    /**********************************************************************************************/
    /**********************************************************************************************/
    /*                          Métodos base de datos                                             */
    /**********************************************************************************************/
    /**
     * ******************************************************************************************
     */
    private ObjectContainer abrirBD() {
        try {
            if (bd == null) {
                bd = Db4oEmbedded.openFile(Db4oEmbedded.newConfiguration(),
                        getExternalFilesDir(null) + "/bd.db4o");
            }
            return bd;
        } catch (Exception e) {
            return null;
        }
    }

    private void borrarBD(Juego j) {
        ObjectSet<Juego> juegos = consulta(j);
        if (juegos.hasNext()) {
            Juego i = juegos.next();
            abrirBD().delete(i);
            abrirBD().commit();
        }
    }

    private ObjectSet<Juego> consulta(final Juego j){
        ObjectSet<Juego> juegos = abrirBD().query(
                new Predicate<Juego>() {
                    @Override
                    public boolean match(Juego p) {
                        return p.compareTo(j) == 0;
                    }
                }
        );
        return juegos;
    }

    private void editarBD(Juego original, Juego nuevo) {
        ObjectSet<Juego> juegos = consulta(original);
        if (juegos.hasNext()) {
            Juego juego = juegos.next();
            juego.copiar(nuevo);
            abrirBD().store(juego);
            abrirBD().commit();
        }
    }

    private void insertarBD(Juego j) {
        abrirBD().store(j);
        abrirBD().commit();
    }

    private void leerDatos() {
        Juego juego = new Juego();
        List<Juego> juegos = abrirBD().queryByExample(juego);
        datosJuegos = new ArrayList(juegos);
    }

    /**********************************************************************************************/
    /**********************************************************************************************/
    /*                                     Auxiliares                                             */
    /**********************************************************************************************/
    /**
     * ******************************************************************************************
     */

    /*Método que actualiza la lista */
    private void actualizarLista() {
        switch (pc.getInt(getString(R.string.ordenarI), 1)) {
            case 1:
                Collections.sort(datosJuegos);
                break;
            case 2:
                Collections.sort(datosJuegos, new JuegosPorPuntuacion());
                break;
            case 3:
                Collections.sort(datosJuegos, new JuegosPorTipo());
                break;
        }
        adaptador.notifyDataSetChanged();
    }

    /*Método para modificar la preferencia compartida ordena y actualizar lista*/
    private boolean cambiaOrdena(int ordena) {
        ed.putInt(getString(R.string.ordenarI), ordena);
        ed.commit();
        actualizarLista();
        return true;
    }

    /*Método para conseguir el drawable correspondiente al tipo de juego*/
    public static int conseguirImagen(Context c, String tipo) {
        int resultado = -1;

        if (tipo.equals(c.getString(R.string.abstracto_tipo))) {
            resultado = R.drawable.abstracto;
        } else if (tipo.equals(c.getString(R.string.ameritrash_tipo))) {
            resultado = R.drawable.ameritrash;
        } else if (tipo.equals(c.getString(R.string.estrategia_tipo))) {
            resultado = R.drawable.estrategia;
        } else if (tipo.equals(c.getString(R.string.eurogame_tipo))) {
            resultado = R.drawable.eurogame;
        } else if (tipo.equals(c.getString(R.string.otro_tipo))) {
            resultado = R.drawable.otro;
        } else if (tipo.equals(c.getString(R.string.tematico_tipo))) {
            resultado = R.drawable.tematico;
        } else if (tipo.equals(c.getString(R.string.wargame_tipo))) {
            resultado = R.drawable.wargame;
        }

        return resultado;
    }

    /*Método que devuelve la información de cada tipo de juego*/
    private String conseguirTexto(String tipo) {
        String resultado = "";

        if (tipo.equals(valoresTipo.get(0))) {
            resultado = getString(R.string.abstracto);
        } else if (tipo.equals(valoresTipo.get(1))) {
            resultado = getString(R.string.ameritrash);
        } else if (tipo.equals(valoresTipo.get(2))) {
            resultado = getString(R.string.estrategia);
        } else if (tipo.equals(valoresTipo.get(3))) {
            resultado = getString(R.string.eurogame);
        } else if (tipo.equals(valoresTipo.get(4))) {
            resultado = getString(R.string.otro);
        } else if (tipo.equals(valoresTipo.get(5))) {
            resultado = getString(R.string.tematico);
        } else if (tipo.equals(valoresTipo.get(6))) {
            resultado = getString(R.string.wargame);
        }

        return resultado;
    }

    /*Método auxiliar para inicializar los componentes de la aplicación*/
    private void initComponents() {
        pc = getSharedPreferences(getString(R.string.preferenciasI), Context.MODE_PRIVATE);
        ed = pc.edit();
        rellenarDatosSpinners();
        datosJuegos = new ArrayList<>();
        leerDatos();
        lista = (ListView) findViewById(R.id.listView);
        adaptador = new AdaptadorLista(this, R.layout.elemento, datosJuegos);
        lista.setAdapter(adaptador);
        actualizarLista();
        lista.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Juego juego = (Juego) adapterView.getItemAtPosition(i);
                posicion = datosJuegos.indexOf(juego);
                mostrarDatos(juego);
            }
        });
        registerForContextMenu(lista);
    }

    /*Rellena los arrayList que se usaran para rellenar los spinners*/
    private void rellenarDatosSpinners() {
        valoresTipo = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.valoresTipo)));
        valoresDificultad = new ArrayList<>();
        valoresPublicacion = new ArrayList<>();
        valoresPuntuacion = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            valoresDificultad.add(String.valueOf(i + 1));
        }
        for (int i = 1979; i < 2014; i++) {
            valoresPublicacion.add(String.valueOf(i + 1));
        }
        valoresPublicacion.add(getString(R.string.otro_tipo));
        for (int i = 0; i < 100; i++) {
            valoresPuntuacion.add(String.valueOf(i + 1));
        }
    }

    /*Metodo que cerrara un diálogo*/
    private void reiniciarDialogo() {
        posicion = -1;
        dialogo = -1;
        alerta.dismiss();
    }

    /*Método que muestra un Toast con el string s*/
    private void tostada(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    /**********************************************************************************************/
    /**********************************************************************************************/
    /*                                        Gestion menus                                       */
    /**********************************************************************************************/
    /**
     * ******************************************************************************************
     */

    /*Método para borrar un juego. pide confirmación*/
    private boolean borrarJuego() {
        dialogo = 2;
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(getString(R.string.borrar_juego));
        LayoutInflater inflater = LayoutInflater.from(this);
        final View vista = inflater.inflate(R.layout.dialogo_borrar, null);
        alert.setView(vista);
        final String nombre = datosJuegos.get(posicion).getNombre() + getString(R.string.parentesis) + datosJuegos.get(posicion).getPublicacion() + getString(R.string.parentesisB);
        TextView texto = (TextView) vista.findViewById(R.id.tvBorrar);
        texto.setText(getString(R.string.seguro) + getString(R.string.espacio) + nombre + getString(R.string.interrogacion));
        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                borrarBD(datosJuegos.get(posicion));
                datosJuegos.remove(posicion);
                actualizarLista();
                tostada(getString(R.string.elemento_borrado) + getString(R.string.espacio) + nombre);
                reiniciarDialogo();
            }
        });
        alert.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                reiniciarDialogo();
            }
        });
        alert.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                reiniciarDialogo();
            }
        });
        alerta = alert.create();
        alerta.show();
        return true;
    }

    /*Metodo para añadir un juego nuevo o editar uno existente.
    Esta versión no permite añadir foto desde la galería.*/
    private boolean nuevoJuego(boolean editar, Juego editable) {
        Intent i = new Intent(this, DialogoInsertar.class);
        Bundle b = new Bundle();
        b.putSerializable(getString(R.string.valoresTipoI), valoresTipo);
        b.putSerializable(getString(R.string.valoresDificultadI), valoresDificultad);
        b.putSerializable(getString(R.string.valoresPuntuacionI), valoresPuntuacion);
        b.putSerializable(getString(R.string.valoresPublicacionI), valoresPublicacion);
        if (editar && editable != null) {
            b.putString(getString(R.string.nombreI), editable.getNombre());
            b.putString(getString(R.string.informacionI), editable.getInformacion());
            b.putString(getString(R.string.publicacionI), editable.getPublicacion());
            b.putString(getString(R.string.tipoI), editable.getTipo());
            b.putInt(getString(R.string.dificultadI), editable.getDificultad());
            b.putInt(getString(R.string.puntuacionI), editable.getPuntuacion());
            b.putBoolean(getString(R.string.expansionI), editable.isExpansion());
            i.putExtras(b);
            startActivityForResult(i, this.EDITAR_JUEGO);
        } else {
            i.putExtras(b);
            startActivityForResult(i, this.NUEVO_JUEGO);
        }
        return true;
    }

    /**********************************************************************************************/
    /**********************************************************************************************/
    /*                                        Clic                                                */
    /**********************************************************************************************/
    /**
     * ******************************************************************************************
     */

    /*Método para mostrar los datos del juego en un diálogo al hacer click en el item de la lista.
    * Muestra la foto según el tipo de juego*/
    public void mostrarDatos(Juego juego) {
        dialogo = 1;
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(juego.getNombre());
        LayoutInflater inflater = LayoutInflater.from(this);
        View vista = inflater.inflate(R.layout.dialogo_mostrar, null);
        alert.setView(vista);

        TextView expansion = (TextView) vista.findViewById(R.id.mostrarExpansion);
        TextView publicacion = (TextView) vista.findViewById(R.id.mostarPublicacion);
        TextView dificultad = (TextView) vista.findViewById(R.id.mostrarDificultad);
        TextView informacion = (TextView) vista.findViewById(R.id.mostrarInfo);
        TextView tipo = (TextView) vista.findViewById(R.id.mostrarTipo);
        TextView puntuacion = (TextView) vista.findViewById(R.id.mostrarPuntuacion);
        ImageView foto = (ImageView) vista.findViewById(R.id.imageMostrar);

        publicacion.setText(String.valueOf(juego.getPublicacion()));
        puntuacion.setText(String.valueOf(juego.getPuntuacion()));
        tipo.setText(juego.getTipo());
        dificultad.setText(String.valueOf(juego.getDificultad()));
        informacion.setText(juego.getInformacion());
        if (juego.isExpansion()) {
            expansion.setText(getString(R.string.si));
        } else {
            expansion.setText(getString(R.string.no));
        }
        if (juego.getFoto() != null) {
            //Si cambio la fotos por Objeto, modificaré esta línea
        } else {
            String tipo2 = juego.getTipo();
            int imagen = conseguirImagen(this, tipo2);
            foto.setImageResource(imagen);
        }

        alert.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                reiniciarDialogo();
            }
        });
        alerta = alert.create();
        alerta.show();
    }

    /* Método que mostrará la foto en grande con el con una descripción del tipo de juego.*/
    public void fotoGrande(View v) {
        posicion = (Integer) v.getTag();
        dialogo = 0;
        mostrarFoto();
    }

    private void mostrarFoto() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        String tipo = datosJuegos.get(posicion).getTipo();
        alert.setTitle(tipo);
        LayoutInflater inflater = LayoutInflater.from(this);
        View vista = inflater.inflate(R.layout.dialogo_foto, null);
        alert.setView(vista);
        ImageView iv = (ImageView) vista.findViewById(R.id.soloTipo);
        TextView tv = (TextView) vista.findViewById(R.id.tipoTexto);

        iv.setImageResource(conseguirImagen(this, tipo));
        tv.setText(conseguirTexto(tipo));

        alert.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                reiniciarDialogo();
            }
        });
        alerta = alert.create();
        alerta.show();
    }
    /**********************************************************************************************/
    /**********************************************************************************************/
    /*                                   Clases                                                   */
    /**********************************************************************************************/
    /**
     * ******************************************************************************************
     */

    /*Clases comparator para ordenar los items de la lista por diferentes campos*/
    private class JuegosPorPuntuacion implements Comparator<Juego> {
        public int compare(Juego o1, Juego o2) {
            int neg = o2.getPuntuacion() - o1.getPuntuacion();
            if (neg == 0) {
                return o1.compareTo(o2);
            }
            return neg;
        }
    }

    private class JuegosPorTipo implements Comparator<Juego> {
        public int compare(Juego o1, Juego o2) {
            int neg = o1.getTipo().compareTo(o2.getTipo());
            if (neg == 0) {
                return o1.compareTo(o2);
            }
            return neg;
        }
    }
}