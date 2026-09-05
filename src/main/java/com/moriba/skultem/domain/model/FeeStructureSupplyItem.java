package com.moriba.skultem.domain.model;

import com.moriba.skultem.application.error.RuleException;

import lombok.Getter;

// One material bundled into a supply fee - a Uniform fee might include the Uniform itself, a
// House Colour, and a Necktie, each its own line with its own quantity, rather than one fee being
// able to carry only a single material. See RecordPaymentUseCase#processSupply, which issues one
// Supply record per line once the fee is fully paid.
@Getter
public class FeeStructureSupplyItem {
    private String id;
    private Material material;
    private int quantity;

    public FeeStructureSupplyItem(String id, Material material, int quantity) {
        this.id = id;
        this.material = material;
        this.quantity = quantity;
    }

    public static FeeStructureSupplyItem create(String id, Material material, int quantity) {
        if (quantity <= 0) {
            throw new RuleException("Supply item quantity must be greater than 0");
        }

        return new FeeStructureSupplyItem(id, material, quantity);
    }
}
