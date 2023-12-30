package dev.tildejustin.minecartturning.mixin;

import net.minecraft.entity.*;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Debug(export = true)
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow
    public abstract float getYaw();

    @Shadow
    public abstract float getPitch();

    @Shadow
    public abstract @Nullable Entity getVehicle();

    @Shadow
    public abstract void setYaw(float yaw);

    @Shadow
    public abstract void setPitch(float pitch);

    @Unique
    private float ridingEntityYawDelta;

    @Unique
    private float ridingEntityPitchDelta;

    @Inject(method = "tickRiding", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;updatePassengerPosition(Lnet/minecraft/entity/Entity;)V", shift = At.Shift.AFTER))
    private void modifyYawAndPitch(CallbackInfo ci) {
        // intellij really doesn't like the "this" check because the nominally disparate class hierarchies at compile-time
        // also Entity#getVehicle is not null at this point in the control flow
        // noinspection ConstantValue, DataFlowIssue
        if (this.getVehicle().getControllingPassenger() == (Object) this) {
            // if this is the controlling passenger, it's already setting the movements of the vehicle
            // for boats, pigs, etc.
            return;
        }

        if (this.getVehicle() instanceof LivingEntity livingVehicle) {
            // if just Entity#getYaw is used, nothing happens to the player camera when an animal turns at standstill
            // body instead of head to match 1.2 behaviour
            this.ridingEntityYawDelta = this.ridingEntityYawDelta + this.getVehicle().getBodyYaw() - livingVehicle.prevBodyYaw;
        } else {
            // from here onwards is just taken from 1.2.5, with minor edits to use getters and such
            this.ridingEntityYawDelta = this.ridingEntityYawDelta + this.getVehicle().getYaw() - this.getVehicle().prevYaw;
        }
        this.ridingEntityPitchDelta = this.ridingEntityPitchDelta + this.getVehicle().getPitch() - this.getVehicle().prevPitch;

        while (this.ridingEntityYawDelta >= 180.0) {
            this.ridingEntityYawDelta -= 360.0F;
        }

        while (this.ridingEntityYawDelta < -180.0) {
            this.ridingEntityYawDelta += 360.0F;
        }

        while (this.ridingEntityPitchDelta >= 180.0) {
            this.ridingEntityPitchDelta -= 360.0F;
        }

        while (this.ridingEntityPitchDelta < -180.0) {
            this.ridingEntityPitchDelta += 360.0F;
        }

        var ridingEntityYawDeltaSmooth = this.ridingEntityYawDelta * 0.5F;
        var ridingEntityPitchDeltaSmooth = this.ridingEntityPitchDelta * 0.5F;

        var maxTurn = 10F;
        if (ridingEntityYawDeltaSmooth > maxTurn) {
            ridingEntityYawDeltaSmooth = maxTurn;
        }

        if (ridingEntityYawDeltaSmooth < -maxTurn) {
            ridingEntityYawDeltaSmooth = -maxTurn;
        }

        if (ridingEntityPitchDeltaSmooth > maxTurn) {
            ridingEntityPitchDeltaSmooth = maxTurn;
        }

        if (ridingEntityPitchDeltaSmooth < -maxTurn) {
            ridingEntityPitchDeltaSmooth = -maxTurn;
        }

        this.ridingEntityYawDelta -= ridingEntityYawDeltaSmooth;
        this.ridingEntityPitchDelta -= ridingEntityPitchDeltaSmooth;
        this.setYaw(this.getYaw() + ridingEntityYawDeltaSmooth);
        this.setPitch(this.getPitch() + ridingEntityPitchDeltaSmooth);
    }

    @Inject(method = "startRiding(Lnet/minecraft/entity/Entity;Z)Z", at = @At("HEAD"))
    private void resetPitchAndDelta(Entity entity, boolean force, CallbackInfoReturnable<Boolean> cir) {
        this.ridingEntityPitchDelta = 0.0F;
        this.ridingEntityYawDelta = 0.0F;
    }
}
