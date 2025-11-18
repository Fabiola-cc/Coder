class Persona {
    let nombre: string;
    let edad: integer;

    function constructor(nombre: string, edad: integer) {
        this.nombre = nombre;
        this.edad = edad;
    }

    function saludar(): string {
        return "Hola " + this.nombre;
    }
}

let p: Persona = new Persona("Juan", 25);
print(p.edad);