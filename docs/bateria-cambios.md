# Batería de cambios (prueba de intentos, 18/10/2026)

Apuntados mientras el usuario prueba la rama de intentos. No se implementan hasta cerrar la batería.

## Publicar un examen

1. **Fecha de caducidad vacía.** Nota del usuario: «en nuevo examen, si dejas vacío la hora, no caduca».
   Hoy, con la fecha vacía, el examen caduca a la hora (RN-04) y «Sin caducidad» es una casilla aparte.
   - Por aclarar: ¿es un fallo visto en la prueba, o un cambio de regla (vacía = sin caducidad, sin casilla)?

2. **Intentos por alumno con una barra.** Cambiar el campo numérico por una barra con cinco posiciones:
   1, 2, 3, 4 o infinito (sin límite). Quitar el texto «De 1 a 20; cuentan también los que se dejan
   sin entregar».
   - Implica: el backend admite «sin límite» y el máximo con número pasa de 20 a 4.
   - La portada diría «Puedes hacerlo las veces que quieras» y el examen, «Intento 3» sin «de N».

3. **Ver las respuestas al terminar un intento.** Opción nueva al programar el examen: el profesor
   elige si el alumno, tras completar un intento, puede ver las respuestas.
   - Toca RF-53 (hoy dice que el alumno nunca ve la respuesta correcta) y la opción «Muestra nota».
   - Por aclarar: ¿«ver las respuestas» es ver sus respuestas marcadas bien o mal, o ver también
     cuál era la correcta?

4. **Tipo de examen por defecto: oficial.** Al publicar, el valor por defecto pasa a ser oficial.
   Textos de las dos opciones:
   - «OFICIAL / PRIVADO: solo con código»
   - «PÚBLICO: examen de prueba, cualquiera puede entrar en cualquier momento»

5. **Editar un examen ya publicado: solo el plazo y los intentos.** El plazo ya se puede cambiar
   («Cambiar plazo»); falta poder cambiar los intentos por alumno después de publicar.

## Montar el examen (selector de preguntas)

6. **Carrito de preguntas:** otro icono que no sea un carrito, y quitar el color del cinturón.

7. **Aviso de filtro por tags:** junto a «X de Y preguntas», poner «(filtrando tags)» cuando haya tags
   marcados, para que el profesor sepa que la lista está filtrada.

8. **Tags en cuadrícula y «Desmarcar tags».** La opción «Sin tag» se cambia por «Desmarcar tags».
   Los tags van en una cuadrícula y «Desmarcar tags» queda fuera de ella, pegado al borde como una
   pestaña o etiqueta.
   - Por aclarar: ¿desaparece el filtro de preguntas sin tag, o se mantiene en otro sitio?
   - ¿Lo mismo en el banco de preguntas (RF-10) o solo en el selector del examen?

## Crear y editar preguntas

9. **Grado mínimo opcional:** etiqueta «Grado mínimo (opcional)»; una pregunta puede no tener grado.
    - Implica: el grado deja de ser obligatorio en el backend (hoy lo es) y en los filtros por grado.

10. **Tipo:** etiqueta «Tipo de examen de arbitraje», con las opciones «Cualquiera», «Tipo C» y «Tipo D»
    («Cualquiera» = sin tipo, lo que hoy es «ninguno»).

11. **Varias respuestas correctas.** Poder marcar más de una opción correcta.
    - Cambio grande: modelo de pregunta, copia publicada (DE-03), corrección, examen del alumno,
      resultados y Excel.
    - Por aclarar: ¿el alumno marca varias casillas? ¿Cómo puntúa: acierto solo si marca exactamente
      todas las correctas, o puntuación parcial? ¿Se avisa al alumno de que hay varias correctas?

## Resultados del examen (profesor)

12. **Ir al examen:** un botón en los resultados de un examen que lleve a ese examen en «Exámenes».

13. **Registrados y anónimos.** Marcar con un color al inicio del nombre si es usuario registrado o no,
   y poner primero a los registrados.
   - Por aclarar: dentro de cada grupo, ¿por fecha (la más reciente primero, como hoy) o por nombre?
   - Relacionado con RF-54 (cada alumno una vez, con sus intentos desplegables).

15. **Etiquetas de registrado.** Las marcas del punto 13 son etiquetas: «Registrado» en rojo y
    «No registrado» en gris.

## Hacer el examen (alumno)

16. **Repetir examen.** Si al alumno le quedan intentos, al terminar ve un botón bien visible a la
    derecha: «Repetir examen», con los intentos que le quedan.

17. **Mismo nombre que un registrado.** Hoy, los intentos de un anónimo que se llama igual que un
    usuario registrado no se suman a los de ese usuario (los registrados se cuentan por cuenta y los
    anónimos por nombre, sin distinguir mayúsculas). El usuario quiere que, si se llama exactamente
    igual, «se acumule como en el resto de usuarios», con la etiqueta «No registrado».
    - Por aclarar: ¿se suman a los intentos del registrado, o solo cuentan entre los anónimos con ese
      nombre? ¿«Exactamente igual» significa distinguir mayúsculas y tildes (hoy no se distinguen)?

## Clubes

14. **No se pueden añadir usuarios al club.** Comprobado (18/10): la API devuelve candidatos (Abel sale
    al buscar en el club 2) y ningún usuario de la base de desarrollo tiene club. El fallo debe de estar
    en la pantalla o en el paso de añadir.
    - Por aclarar: con qué cuenta, y qué pasa: no aparece «Añadir miembros», la búsqueda no encuentra
      a nadie o falla al pulsar «Añadir» (¿con qué mensaje?).

## Decisiones (18/10/2026)

- Punto 1: fecha vacía = sin caducidad; se quita la casilla «Sin caducidad».
- Punto 3: el profesor elige «Mostrar»: nada, nota, o nota y respuestas (sus respuestas en verde o rojo,
  nunca la correcta, como dice RF-53).
- Punto 5: los intentos se pueden bajar después de publicar; quien ya los haya usado no empieza otro.
- Punto 16: «Repetir examen» también para anónimos; empieza otro intento con los mismos datos.
- Punto 17: el anónimo solo cuenta entre los anónimos con ese nombre, sin distinguir mayúsculas ni tildes.
- Punto 13: primero los registrados y dentro de cada grupo por nombre; RF-54 a la vez.
- Punto 11: el alumno marca casillas y la pregunta avisa «Marca todas las correctas»; acierta solo si
  marca exactamente las correctas.
- Punto 8: se quita «Sin tag», en el banco y en el selector del examen.
- Punto 14 aclarado: en la portada del examen, el alumno anónimo no puede elegir su club de un
  desplegable (es un campo de texto). Comprobar también al registrado y arreglarlo.

## Orden de trabajo

1. Lote de interfaz: club en la portada, tipo por defecto y textos, icono y cinturón del carrito,
   «(filtrando tags)», «Desmarcar tags», tipo de arbitraje, «Ir al examen», etiquetas de registrado.
2. Cambios de modelo: caducidad, barra de intentos con sin límite, editar intentos, «Mostrar», «Repetir
   examen», grado opcional, RF-54.
3. Varias respuestas correctas.
