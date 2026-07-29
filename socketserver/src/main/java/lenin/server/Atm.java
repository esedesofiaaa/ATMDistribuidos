package lenin.server;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Atm — logica de negocio del cajero (fat server).
 *
 * Toda la validacion vive aqui: montos no numericos, montos no positivos y
 * fondos insuficientes. El cliente no valida nada, solo muestra la respuesta.
 *
 * No sabe nada de sockets ni del protocolo: recibe y devuelve datos. El estado
 * (el saldo) es unico y sobrevive entre conexiones mientras el servidor viva.
 */
public class Atm {

  private BigDecimal saldo;

  public Atm(String saldoInicial) {
    this.saldo = new BigDecimal(saldoInicial).setScale(2, RoundingMode.HALF_UP);
  }

  /** Consulta el saldo actual, formateado con dos decimales. */
  public String consultarSaldo() {
    return this.saldo.toPlainString();
  }

  /**
   * Suma el monto al saldo.
   *
   * @return null si la operacion fue valida, o el motivo del rechazo.
   */
  public String depositar(String montoTexto) {
    BigDecimal monto = parsearMonto(montoTexto);
    if (monto == null) {
      return "El monto '" + montoTexto + "' no es un numero valido";
    }
    if (monto.signum() <= 0) {
      return "El monto a depositar debe ser mayor que cero";
    }

    this.saldo = this.saldo.add(monto);
    return null;
  }

  /**
   * Resta el monto del saldo si hay fondos suficientes.
   *
   * @return null si la operacion fue valida, o el motivo del rechazo.
   */
  public String retirar(String montoTexto) {
    BigDecimal monto = parsearMonto(montoTexto);
    if (monto == null) {
      return "El monto '" + montoTexto + "' no es un numero valido";
    }
    if (monto.signum() <= 0) {
      return "El monto a retirar debe ser mayor que cero";
    }
    if (monto.compareTo(this.saldo) > 0) {
      return "Fondos insuficientes. Saldo disponible: " + this.saldo.toPlainString();
    }

    // La validacion y la mutacion ocurren juntas: la operacion se aplica
    // completa o no se aplica en absoluto.
    this.saldo = this.saldo.subtract(monto);
    return null;
  }

  private BigDecimal parsearMonto(String montoTexto) {
    if (montoTexto == null || montoTexto.isBlank()) {
      return null;
    }
    try {
      return new BigDecimal(montoTexto.trim()).setScale(2, RoundingMode.HALF_UP);
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
